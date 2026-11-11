package com.floralshop.backend.catalog.domain.exception;

// Se lanza cuando el carrito trae una variante que no existe o no está a la venta
// (inactiva, o de un producto inactivo).
public class VariantNotAvailableException extends RuntimeException {

    public VariantNotAvailableException(long variantId) {
        super("La variante con id " + variantId + " no está disponible");
    }
}