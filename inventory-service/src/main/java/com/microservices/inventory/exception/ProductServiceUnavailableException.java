package com.microservices.inventory.exception;

/**
 * Product Service Unavailable Exception
 *
 * Thrown when product-service cannot be reached or returns an unexpected
 * error while inventory-service is validating a SKU on the write path
 *
 * @author Microservices Team
 * @version 1.0
 */
public class ProductServiceUnavailableException extends RuntimeException {

    public ProductServiceUnavailableException() {
        super("Product catalog service is currently unavailable");
    }

    public ProductServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
