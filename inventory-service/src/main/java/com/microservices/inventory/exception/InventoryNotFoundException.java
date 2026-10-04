package com.microservices.inventory.exception;

/**
 * Inventory Not Found Exception
 *
 * Thrown when a requested inventory record does not exist
 *
 * @author Microservices Team
 * @version 1.0
 */
public class InventoryNotFoundException extends RuntimeException {

    /**
     * Constructs exception with inventory record ID
     *
     * @param id Inventory record ID that was not found
     */
    public InventoryNotFoundException(Long id) {
        super(String.format("Inventory record not found with id: %d", id));
    }

    /**
     * Constructs exception with SKU code
     *
     * @param skuCode SKU code that was not found
     */
    public InventoryNotFoundException(String skuCode) {
        super(String.format("Inventory record not found with SKU code: %s", skuCode));
    }

    /**
     * Constructs exception with custom message
     *
     * @param message Custom error message
     * @param args Message formatting arguments
     */
    public InventoryNotFoundException(String message, Object... args) {
        super(String.format(message, args));
    }
}
