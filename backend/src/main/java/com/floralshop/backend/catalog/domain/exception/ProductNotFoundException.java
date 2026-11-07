package com.floralshop.backend.catalog.domain.exception;

// Se lanza cuando se pide un producto que no existe o no está activo.
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String slug) {
        super("No existe el producto con slug '" + slug + "'");
    }
}