package com.floralshop.backend.scheduling.infra.adapter.out.persistence;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Entidad JPA: refleja la tabla delivery_slot. Es un detalle de infraestructura.
// El negocio nunca la ve: usa DeliverySlot (dominio), y el mapper convierte entre las dos.
@Entity
@Table(name = "delivery_slot")
public class DeliverySlotJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // la base numera sola (GENERATED ALWAYS AS IDENTITY)
    private Long id;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private int booked;

    @Column(name = "is_open", nullable = false) // la columna se llama is_open; el campo, open
    private boolean open;

    // JPA exige un constructor sin argumentos para poder crear la entidad al leer de la base.
    protected DeliverySlotJpaEntity() {
    }

    public DeliverySlotJpaEntity(Long id, LocalDate slotDate, LocalTime startTime, LocalTime endTime,
                              int capacity, int booked, boolean open) {
        this.id = id;
        this.slotDate = slotDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.capacity = capacity;
        this.booked = booked;
        this.open = open;
    }

    public Long getId() { return id; }
    public LocalDate getSlotDate() { return slotDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public int getCapacity() { return capacity; }
    public int getBooked() { return booked; }
    public boolean isOpen() { return open; }
}