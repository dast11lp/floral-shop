package com.floralshop.backend.scheduling.domain.model;

import java.util.Objects;

// Una franja vista desde un carrito: available dice si ese carrito la puede tomar.
public record SlotOption(DeliverySlot slot, boolean available) {

    public SlotOption {
        Objects.requireNonNull(slot, "La franja es obligatoria");
    }
}