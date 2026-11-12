package com.floralshop.backend.ordering.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.floralshop.backend.catalog.application.service.CatalogQueryService;
import com.floralshop.backend.catalog.domain.exception.VariantNotAvailableException;
import com.floralshop.backend.catalog.domain.model.CartLine;
import com.floralshop.backend.catalog.domain.model.OrderableVariant;
import com.floralshop.backend.ordering.application.port.out.OrderRepositoryPort;
import com.floralshop.backend.ordering.application.port.out.OrderSettingsRepositoryPort;
import com.floralshop.backend.ordering.domain.exception.OrderLimitExceededException;
import com.floralshop.backend.ordering.domain.model.CustomerContact;
import com.floralshop.backend.ordering.domain.model.NewOrder;
import com.floralshop.backend.ordering.domain.model.Order;
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

// Crear un pedido: valida, copia lo comprado, aparta los cupos y lo deja pendiente de pago.
@ExtendWith(MockitoExtension.class)
class CreateOrderServiceTest {

    @Mock OrderSettingsRepositoryPort settings;
    @Mock OrderRepositoryPort orders;
    @Mock CatalogQueryService catalog;
    @Mock SchedulingQueryService scheduling;
    @Mock DeliveryOptionsService deliveryOptions;
    @Mock ReserveSlotService reserveSlot;

    CreateOrderService service;

    private static final Instant NOW = Instant.parse("2026-12-10T15:00:00Z");

    private final CustomerContact customer = new CustomerContact("Ana", "ana@example.com", "3000000000");
    // Ramo grande: 48 h propias, 2 cupos por unidad, $130.000
    private final OrderableVariant big = new OrderableVariant(1L, "Ramo de rosas", "Grande",
            new BigDecimal("130000"), 48, 4, 2);
    private final DeliverySlot slot = new DeliverySlot(9L, LocalDate.of(2026, 12, 18), LocalTime.of(9, 0),
            LocalTime.of(11, 0), 4, 0, true);
    private final DeliveryZone zone = new DeliveryZone(7L, "Chía", new BigDecimal("15000"), true, 1);

    @BeforeEach
    void setUp() {
        service = new CreateOrderService(settings, orders, catalog, scheduling, deliveryOptions, reserveSlot,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private NewOrder request(long slotId, CartLine... lines) {
        return new NewOrder(customer, "Calle 1 # 2-3", 7L, slotId, List.of(lines));
    }

    private void limits(int perLine, int perOrder) {
        when(settings.get()).thenReturn(new OrderSettings(perLine, perOrder, 15, 51));
    }

    private void slotIs(boolean available) {
        // 2 ramos grandes: 96 h y 4 cupos
        when(deliveryOptions.optionsFor(List.of(new CartLine(1L, 2)), 7L)).thenReturn(new DeliveryOptions(
                96, 4, LocalDateTime.of(2026, 12, 18, 9, 0), List.of(new SlotOption(slot, available))));
    }

    @Test
    void createsAPendingOrderWithACopyOfWhatWasBoughtAndReservesTheSlotUnits() {
        limits(10, 20);
        when(catalog.orderableVariants(List.of(1L))).thenReturn(List.of(big));
        slotIs(true);
        when(scheduling.getActiveZone(7L)).thenReturn(zone);
        when(orders.save(any())).thenAnswer(call -> call.getArgument(0));

        Order order = service.create(request(9L, new CartLine(1L, 2)));

        verify(reserveSlot).reserve(9L, 4); // los cupos del carrito, no las unidades
        assertThat(order.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(order.publicCode()).isNotNull();
        assertThat(order.zoneName()).isEqualTo("Chía");
        assertThat(order.slotDate()).isEqualTo(LocalDate.of(2026, 12, 18));
        assertThat(order.craftingHours()).isEqualTo(96);
        assertThat(order.subtotal()).isEqualByComparingTo("260000");
        assertThat(order.total()).isEqualByComparingTo("275000");
        assertThat(order.lines()).singleElement().satisfies(line -> {
            assertThat(line.productName()).isEqualTo("Ramo de rosas");
            assertThat(line.variantName()).isEqualTo("Grande");
            assertThat(line.unitPrice()).isEqualByComparingTo("130000");
            assertThat(line.craftingHours()).isEqualTo(48);
            assertThat(line.slotUnits()).isEqualTo(2);
        });
        // La franja queda retenida 15 minutos para empezar a pagar, con tope de 51 (order_settings).
        assertThat(order.holdExpiresAt()).isEqualTo(NOW.plusSeconds(15 * 60));
        assertThat(order.holdLimitAt()).isEqualTo(NOW.plusSeconds(51 * 60));
    }

    @Test
    void lineOverTheLimitIsRejectedBeforeTouchingAnything() {
        limits(2, 20);

        assertThatThrownBy(() -> service.create(request(9L, new CartLine(1L, 3))))
                .isInstanceOf(OrderLimitExceededException.class);
        verifyNoInteractions(catalog, deliveryOptions, reserveSlot, orders);
    }

    @Test
    void orderOverTheLimitIsRejected() {
        limits(10, 5);

        assertThatThrownBy(() -> service.create(request(9L, new CartLine(1L, 3), new CartLine(2L, 3))))
                .isInstanceOf(OrderLimitExceededException.class);
        verifyNoInteractions(reserveSlot, orders);
    }

    @Test
    void unavailableVariantIsRejectedWithoutReserving() {
        limits(10, 20);
        when(catalog.orderableVariants(List.of(1L))).thenReturn(List.of());

        assertThatThrownBy(() -> service.create(request(9L, new CartLine(1L, 2))))
                .isInstanceOf(VariantNotAvailableException.class);
        verifyNoInteractions(reserveSlot, orders);
    }

    @Test
    void slotThatDoesNotFitTheCartIsRejectedWithoutReserving() {
        limits(10, 20);
        when(catalog.orderableVariants(List.of(1L))).thenReturn(List.of(big));
        slotIs(false); // muy pronto, cerrada o sin cupos para este carrito

        assertThatThrownBy(() -> service.create(request(9L, new CartLine(1L, 2))))
                .isInstanceOf(SlotNotAvailableException.class);
        verify(reserveSlot, never()).reserve(anyLong(), anyInt());
        verifyNoInteractions(orders);
    }

    @Test
    void slotOutsideTheOptionsIsRejected() {
        limits(10, 20);
        when(catalog.orderableVariants(List.of(1L))).thenReturn(List.of(big));
        slotIs(true);

        assertThatThrownBy(() -> service.create(request(99L, new CartLine(1L, 2))))
                .isInstanceOf(SlotNotAvailableException.class);
        verifyNoInteractions(reserveSlot, orders);
    }

    @Test
    void ifAnotherCustomerTookTheLastUnitsNothingIsSaved() {
        limits(10, 20);
        when(catalog.orderableVariants(List.of(1L))).thenReturn(List.of(big));
        slotIs(true);
        when(scheduling.getActiveZone(7L)).thenReturn(zone);
        when(reserveSlot.reserve(9L, 4)).thenThrow(new SlotNotAvailableException("No hay cupo suficiente"));

        assertThatThrownBy(() -> service.create(request(9L, new CartLine(1L, 2))))
                .isInstanceOf(SlotNotAvailableException.class);
        verifyNoInteractions(orders);
    }

    @Test
    void eachOrderGetsItsOwnPublicCode() {
        limits(10, 20);
        when(catalog.orderableVariants(List.of(1L))).thenReturn(List.of(big));
        slotIs(true);
        when(scheduling.getActiveZone(7L)).thenReturn(zone);
        when(orders.save(any())).thenAnswer(call -> call.getArgument(0));
        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);

        service.create(request(9L, new CartLine(1L, 2)));
        service.create(request(9L, new CartLine(1L, 2)));

        verify(orders, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues().get(0).publicCode())
                .isNotEqualTo(saved.getAllValues().get(1).publicCode());
    }
}