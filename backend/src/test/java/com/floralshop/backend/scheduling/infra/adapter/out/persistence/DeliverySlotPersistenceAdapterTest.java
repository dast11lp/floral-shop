package com.floralshop.backend.scheduling.infra.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import com.floralshop.backend.TestcontainersConfiguration;
import com.floralshop.backend.scheduling.application.port.out.DeliverySlotRepositoryPort;
import com.floralshop.backend.scheduling.domain.model.DeliverySlot;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

// Prueba de integración: Spring completo + Postgres real (Testcontainers).
// @Transactional: cada prueba corre dentro de una transacción que se deshace al terminar,
// así no deja datos. Además, el bloqueo FOR UPDATE exige estar dentro de una transacción.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class DeliverySlotPersistenceAdapterTest {

    @Autowired
    DeliverySlotRepositoryPort slots; // el PUERTO, no el adaptador: probamos lo que usará el negocio

    @PersistenceContext
    EntityManager em;

    private DeliverySlot newSlot(int capacity, int booked, boolean open) {
        return new DeliverySlot(null, LocalDate.of(2026, 12, 24),
                LocalTime.of(9, 0), LocalTime.of(11, 0), capacity, booked, open);
    }

    // Manda lo pendiente a la base y vacía la memoria de JPA, para que la
    // siguiente lectura vaya de verdad a Postgres y no devuelva el mismo objeto en memoria.
    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Test
    void savingNewSlotAssignsId() {
        DeliverySlot saved = slots.save(newSlot(10, 0, true));

        assertThat(saved.id()).isNotNull();
        assertThat(saved.capacity()).isEqualTo(10);
    }

    @Test
    void findByIdForUpdateReturnsWhatWasSaved() {
        DeliverySlot saved = slots.save(newSlot(10, 3, true));
        flushAndClear();

        DeliverySlot found = slots.findByIdForUpdate(saved.id()).orElseThrow();

        assertThat(found).isEqualTo(saved); // los records se comparan campo a campo
    }

    @Test
    void findByIdForUpdateReturnsEmptyWhenMissing() {
        assertThat(slots.findByIdForUpdate(-1L)).isEmpty();
    }

    @Test
    void savingExistingSlotUpdatesIt() {
        DeliverySlot saved = slots.save(newSlot(10, 0, true));
        slots.save(new DeliverySlot(saved.id(), saved.date(), saved.startTime(), saved.endTime(),
                saved.capacity(), 4, saved.open())); // misma franja, con 4 reservados
        flushAndClear();

        assertThat(slots.findByIdForUpdate(saved.id()).orElseThrow().booked()).isEqualTo(4);
    }

    @Test
    void closedSlotIsStoredAndReadBackAsClosed() {
        DeliverySlot saved = slots.save(newSlot(10, 0, false));
        flushAndClear();

        assertThat(slots.findByIdForUpdate(saved.id()).orElseThrow().open()).isFalse();
    }
}