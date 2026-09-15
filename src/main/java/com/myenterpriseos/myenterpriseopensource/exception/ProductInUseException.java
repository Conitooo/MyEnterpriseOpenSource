package com.myenterpriseos.myenterpriseopensource.exception;

public class ProductInUseException extends RuntimeException {

    public ProductInUseException(Long id, Throwable cause) {
        super("Cannot delete product with id: " + id + " because it is referenced by inventory or order items", cause);
    }
}
