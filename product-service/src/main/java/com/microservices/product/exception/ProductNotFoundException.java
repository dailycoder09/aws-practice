package com.microservices.product.exception;

/**
 * Product Not Found Exception
 * 
 * Thrown when a requested product does not exist
 * 
 * @author Microservices Team
 * @version 1.0
 */
public class ProductNotFoundException extends RuntimeException {

    /**
     * Constructs exception with product ID
     * 
     * @param id Product ID that was not found
     */
    public ProductNotFoundException(Long id) {
        super(String.format("Product not found with id: %d", id));
    }

    /**
     * Constructs exception with SKU code
     * 
     * @param skuCode SKU code that was not found
     */
    public ProductNotFoundException(String skuCode) {
        super(String.format("Product not found with SKU code: %s", skuCode));
    }

    /**
     * Constructs exception with custom message
     * 
     * @param message Custom error message
     * @param args Message formatting arguments
     */
    public ProductNotFoundException(String message, Object... args) {
        super(String.format(message, args));
    }
}
