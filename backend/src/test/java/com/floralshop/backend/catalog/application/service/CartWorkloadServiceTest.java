package com.floralshop.backend.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.floralshop.backend.catalog.application.port.out.CatalogRepositoryPort;
import com.floralshop.backend.catalog.domain.exception.VariantNotAvailableException;
import com.floralshop.backend.catalog.domain.model.CartLine;
import com.floralshop.backend.catalog.domain.model.CartWorkload;
import com.floralshop.backend.catalog.domain.model.VariantWorkload;

// La regla del carrito (ADR 0012, punto 3): las horas y los cupos se suman, unidad por unidad.
@ExtendWith(MockitoExtension.class)
class CartWorkloadServiceTest {

    @Mock
    CatalogRepositoryPort catalog;

    CartWorkloadService service;

    // Ramo pequeño: sin tiempo propio, hereda las 4 h del producto; 1 cupo.
    private final VariantWorkload small = new VariantWorkload(1L, null, 4, 1);
    // Ramo de boda: 48 h propias; 3 cupos.
    private final VariantWorkload wedding = new VariantWorkload(2L, 48, 4, 3);
    // Girasoles: heredan 6 h de su producto; 1 cupo.
    private final VariantWorkload sunflowers = new VariantWorkload(3L, null, 6, 1);

    @BeforeEach
    void setUp() {
        service = new CartWorkloadService(catalog);
    }

    @Test
    void singleLineUsesTheVariantTimeAndSlotUnits() {
        when(catalog.findAvailableWorkloads(List.of(2L))).thenReturn(List.of(wedding));

        assertThat(service.estimate(List.of(new CartLine(2L, 1))))
                .isEqualTo(new CartWorkload(48, 3));
    }

    @Test
    void linesAreAddedUp() {
        // 4 h (pequeño, heredado) + 48 h (boda) = 52 h; 1 + 3 = 4 cupos
        when(catalog.findAvailableWorkloads(List.of(1L, 2L))).thenReturn(List.of(small, wedding));

        assertThat(service.estimate(List.of(new CartLine(1L, 1), new CartLine(2L, 1))))
                .isEqualTo(new CartWorkload(52, 4));
    }

    @Test
    void quantityMultiplies() {
        // 3 ramos pequeños: 3 x 4 h = 12 h y 3 cupos
        when(catalog.findAvailableWorkloads(List.of(1L))).thenReturn(List.of(small));

        assertThat(service.estimate(List.of(new CartLine(1L, 3))))
                .isEqualTo(new CartWorkload(12, 3));
    }

    @Test
    void linesFromDifferentProductsAreAddedUp() {
        // 2 girasoles (2 x 6 h) + 1 pequeño (4 h) = 16 h; 3 cupos
        when(catalog.findAvailableWorkloads(List.of(3L, 1L))).thenReturn(List.of(sunflowers, small));

        assertThat(service.estimate(List.of(new CartLine(3L, 2), new CartLine(1L, 1))))
                .isEqualTo(new CartWorkload(16, 3));
    }

    @Test
    void sameVariantTwiceIsCountedTwiceButLookedUpOnce() {
        when(catalog.findAvailableWorkloads(List.of(1L))).thenReturn(List.of(small));

        assertThat(service.estimate(List.of(new CartLine(1L, 1), new CartLine(1L, 2))))
                .isEqualTo(new CartWorkload(12, 3));
        verify(catalog).findAvailableWorkloads(List.of(1L));
    }

    @Test
    void unknownOrUnavailableVariantIsRejected() {
        // El puerto no la devuelve: no existe, está inactiva o su producto lo está
        when(catalog.findAvailableWorkloads(anyCollection())).thenReturn(List.of());

        assertThatThrownBy(() -> service.estimate(List.of(new CartLine(99L, 1))))
                .isInstanceOf(VariantNotAvailableException.class)
                .hasMessageContaining("99");
    }

    @Test
    void emptyCartIsRejected() {
        assertThatThrownBy(() -> service.estimate(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}