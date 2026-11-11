package com.floralshop.backend.ordering.application.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.floralshop.backend.catalog.application.service.CatalogQueryService;
import com.floralshop.backend.catalog.domain.exception.VariantNotAvailableException;
import com.floralshop.backend.catalog.domain.model.CartLine;
import com.floralshop.backend.catalog.domain.model.OrderableVariant;
import com.floralshop.backend.ordering.application.port.out.OrderRepositoryPort;
import com.floralshop.backend.ordering.application.port.out.OrderSettingsRepositoryPort;
import com.floralshop.backend.ordering.domain.exception.OrderLimitExceededException;
import com.floralshop.backend.ordering.domain.model.NewOrder;
import com.floralshop.backend.ordering.domain.model.Order;
import com.floralshop.backend.ordering.domain.model.OrderLine;
import com.floralshop.backend.ordering.domain.model.OrderSettings;
import com.floralshop.backend.ordering.domain.model.OrderStatus;
import com.floralshop.backend.scheduling.application.service.DeliveryOptionsService;
import com.floralshop.backend.scheduling.application.service.ReserveSlotService;
import com.floralshop.backend.scheduling.application.service.SchedulingQueryService;
import com.floralshop.backend.scheduling.domain.exception.SlotNotAvailableException;
import com.floralshop.backend.scheduling.domain.model.DeliveryOptions;
import com.floralshop.backend.scheduling.domain.model.DeliverySlot;
import com.floralshop.backend.scheduling.domain.model.DeliveryZone;
import com.floralshop.backend.scheduling.domain.model.SlotOption;

// Caso de uso: crear un pedido como invitado y apartar su cupo (ADR 0012 y 0013). Java puro.
// La transacción que hace que pedido y cupo se guarden o se deshagan juntos se declara en
// infra/config (TransactionalCreateOrderService, ADR 0011).
//
// No repite reglas de otros módulos: el tiempo y los cupos del carrito, y si la franja alcanza,
// los decide DeliveryOptionsService; apartar el cupo, ReserveSlotService.
public class CreateOrderService {

    private final OrderSettingsRepositoryPort settings;
    private final OrderRepositoryPort orders;
    private final CatalogQueryService catalog;
    private final SchedulingQueryService scheduling;
    private final DeliveryOptionsService deliveryOptions;
    private final ReserveSlotService reserveSlot;

    public CreateOrderService(OrderSettingsRepositoryPort settings, OrderRepositoryPort orders,
                              CatalogQueryService catalog, SchedulingQueryService scheduling,
                              DeliveryOptionsService deliveryOptions, ReserveSlotService reserveSlot) {
        this.settings = settings;
        this.orders = orders;
        this.catalog = catalog;
        this.scheduling = scheduling;
        this.deliveryOptions = deliveryOptions;
        this.reserveSlot = reserveSlot;
    }

    public Order create(NewOrder request) {
        // 1. Los máximos de la florista, antes de consultar nada más.
        checkLimits(request.lines(), settings.get());

        // 2. Lo que se va a copiar de cada variante. Si alguna no está a la venta, no hay pedido.
        Map<Long, OrderableVariant> variants = catalog.orderableVariants(
                        request.lines().stream().map(CartLine::variantId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(OrderableVariant::variantId, Function.identity()));
        List<OrderLine> lines = request.lines().stream()
                .map(line -> toOrderLine(line, variants))
                .toList();

        // 3. ¿La franja elegida alcanza para este carrito y esta zona? (zona inválida: excepción)
        DeliveryOptions options = deliveryOptions.optionsFor(request.lines(), request.zoneId());
        DeliverySlot slot = options.slots().stream()
                .filter(option -> option.slot().id() == request.slotId())
                .filter(SlotOption::available)
                .map(SlotOption::slot)
                .findFirst()
                .orElseThrow(() -> new SlotNotAvailableException("La franja elegida no está disponible para este pedido"));
        DeliveryZone zone = scheduling.getActiveZone(request.zoneId());

        // 4. Apartar los cupos. Es atómico: si otro cliente tomó el último cupo, falla aquí.
        reserveSlot.reserve(slot.id(), options.slotUnits());

        // 5. Guardar el pedido, pendiente de pago.
        return orders.save(new Order(null, UUID.randomUUID(), OrderStatus.PENDING_PAYMENT, request.customer(),
                request.deliveryAddress(), zone.id(), zone.name(), zone.shippingFee(),
                slot.id(), slot.date(), slot.startTime(), slot.endTime(),
                options.craftingHours(), options.slotUnits(), lines));
    }

    private static void checkLimits(List<CartLine> lines, OrderSettings limits) {
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("El pedido no tiene líneas");
        }
        int total = 0;
        for (CartLine line : lines) {
            if (line.quantity() > limits.maxUnitsPerLine()) {
                throw new OrderLimitExceededException("Máximo " + limits.maxUnitsPerLine() + " unidades por producto");
            }
            total += line.quantity();
        }
        if (total > limits.maxUnitsPerOrder()) {
            throw new OrderLimitExceededException("Máximo " + limits.maxUnitsPerOrder() + " unidades por pedido");
        }
    }

    private static OrderLine toOrderLine(CartLine line, Map<Long, OrderableVariant> variants) {
        OrderableVariant variant = variants.get(line.variantId());
        if (variant == null) {
            throw new VariantNotAvailableException(line.variantId());
        }
        return new OrderLine(variant.variantId(), variant.productName(), variant.variantName(), variant.price(),
                line.quantity(), variant.craftingHours(), variant.slotUnits());
    }
}