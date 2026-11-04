package com.floralshop.backend.scheduling.infra.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.floralshop.backend.TestcontainersConfiguration;
import com.floralshop.backend.scheduling.application.port.out.DeliverySlotRepositoryPort;
import com.floralshop.backend.scheduling.application.service.ReserveSlotService;
import com.floralshop.backend.scheduling.domain.exception.SlotNotAvailableException;
import com.floralshop.backend.scheduling.domain.exception.SlotNotFoundException;
import com.floralshop.backend.scheduling.domain.model.DeliverySlot;

// Prueba de integración del caso de uso completo: bean real + transacción + Postgres.
// NOTA:NO lleva @Transactional. Debe ser el servicio quien abra la transacción;
// si la prueba la abriera, ocultaría una transacción que faltara en el servicio.
// Como los datos se guardan de verdad, cada prueba borra lo que crea.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ReserveSlotServiceIntegrationTest {

    @Autowired
    ReserveSlotService service; // el bean registrado en SchedulingBeanConfig

    @Autowired
    DeliverySlotRepositoryPort slots;

    @Autowired
    JdbcTemplate jdbc; // para leer y borrar directo en la base, sin pasar por el negocio

    private final List<Long> createdIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        createdIds.forEach(id -> jdbc.update("delete from delivery_slot where id = ?", id));
        createdIds.clear();
    }

    // Fecha distinta de la de otras pruebas: la tabla tiene UNIQUE (slot_date, start_time).
    private long createSlot(int capacity, int booked) {
        DeliverySlot saved = slots.save(new DeliverySlot(null, LocalDate.of(2027, 1, 15),
                LocalTime.of(9, 0), LocalTime.of(11, 0), capacity, booked, true));
        createdIds.add(saved.id());
        return saved.id();
    }

    private int bookedInDatabase(long id) {
        return jdbc.queryForObject("select booked from delivery_slot where id = ?", Integer.class, id);
    }

    @Test
    void serviceBeanIsWrappedInTransactionProxy() {
        // Si alguien quita el @Transactional o el bean, esta prueba lo avisa directamente.
        assertThat(AopUtils.isAopProxy(service)).isTrue();
    }

    @Test
    void reserveUpdatesBookedInDatabase() {
        long id = createSlot(10, 3);

        service.reserve(id, 2);

        assertThat(bookedInDatabase(id)).isEqualTo(5);
    }

    @Test
    void reserveFailsWhenFullAndLeavesDatabaseUnchanged() {
        long id = createSlot(2, 2);

        assertThatThrownBy(() -> service.reserve(id, 1))
                .isInstanceOf(SlotNotAvailableException.class);

        assertThat(bookedInDatabase(id)).isEqualTo(2);
    }

    @Test
    void reserveFailsWhenSlotDoesNotExist() {
        assertThatThrownBy(() -> service.reserve(-1L, 1))
                .isInstanceOf(SlotNotFoundException.class);
    }
}