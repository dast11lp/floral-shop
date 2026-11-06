package com.floralshop.backend.scheduling.infra.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.floralshop.backend.TestcontainersConfiguration;
import com.floralshop.backend.scheduling.application.port.out.DeliverySlotRepositoryPort;
import com.floralshop.backend.scheduling.application.service.ReserveSlotService;
import com.floralshop.backend.scheduling.domain.exception.SlotNotAvailableException;
import com.floralshop.backend.scheduling.domain.model.DeliverySlot;

// Prueba de concurrencia: 50 peticiones simultáneas para 10 cupos.
// Sin @Transactional en la clase: cada petición debe abrir su propia transacción,
// como en producción. Los datos se guardan de verdad y se borran al terminar.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ReserveSlotConcurrencyTest {

    private static final int REQUESTS = 50;
    private static final int CAPACITY = 10;

    @Autowired
    ReserveSlotService service;

    @Autowired
    DeliverySlotRepositoryPort slots;

    @Autowired
    JdbcTemplate jdbc;

    private long slotId;

    @BeforeEach
    void createSlot() {
        // Fecha distinta de la de otras pruebas: la tabla tiene UNIQUE (slot_date, start_time).
        slotId = slots.save(new DeliverySlot(null, LocalDate.of(2027, 2, 20),
                LocalTime.of(9, 0), LocalTime.of(11, 0), CAPACITY, 0, true)).id();
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("delete from delivery_slot where id = ?", slotId);
    }

    @Test
    void fiftyConcurrentRequestsForTenSlotsSellExactlyTen() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(REQUESTS);
        CountDownLatch ready = new CountDownLatch(REQUESTS); // cuenta los hilos que ya están listos
        CountDownLatch go = new CountDownLatch(1);           // la señal de salida
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < REQUESTS; i++) {
            results.add(pool.submit(() -> {
                ready.countDown(); // "se va alistando uno por uno desde el hilo principal hasta que todos estén listos"
                go.await();        // espera la señal, para que todos "salgan a la vez" (el que gane o pierda, no importa)
                try {
                    service.reserve(slotId, 1);
                    return true;   // consiguió cupo
                } catch (SlotNotAvailableException e) {
                    return false;  // rechazada: ya no quedaba cupo
                }
            }));
        }

        ready.await();  // espera a que los 50 hilos estén listos
        go.countDown(); // ¡ya! todos a la vez

        int successes = 0;
        for (Future<Boolean> result : results) {
            // Si un hilo falló por otra causa (interbloqueo, timeout...), get() lanza la excepción y la prueba falla.
            if (result.get(30, TimeUnit.SECONDS)) { // parametro1: cuanto esperamos, parametro2: unidad de tiempo
                successes++;
            }
        }
        pool.shutdown();

        assertThat(successes).isEqualTo(CAPACITY);
        assertThat(bookedInDatabase()).isEqualTo(CAPACITY);
    }

    private int bookedInDatabase() {
        return jdbc.queryForObject("select booked from delivery_slot where id = ?", Integer.class, slotId);
    }
}