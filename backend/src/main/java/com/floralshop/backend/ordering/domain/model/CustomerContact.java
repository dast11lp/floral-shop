package com.floralshop.backend.ordering.domain.model;

import java.util.Objects;

// Quién compra (como invitado, sin cuenta) y cómo contactarlo. Son datos personales (Ley 1581).
public record CustomerContact(String name, String email, String phone) {

    public CustomerContact {
        Objects.requireNonNull(name, "El nombre es obligatorio");
        Objects.requireNonNull(email, "El correo es obligatorio");
        Objects.requireNonNull(phone, "El teléfono es obligatorio");
        if (name.isBlank() || email.isBlank() || phone.isBlank()) {
            throw new IllegalArgumentException("Nombre, correo y teléfono no pueden estar vacíos");
        }
    }
}