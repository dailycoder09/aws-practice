package com.microservices.inventory.exception;

/**
 * Duplicate Inventory Exception
 *
 * Thrown when attempting to create an inventory record for a SKU that
 * already has one
 *
 * @author Microservices Team
 * @version 1.0
 */
public class DuplicateInventoryException extends RuntimeException {

    /**
     * Constructs exception with SKU code
     *
     * @param skuCode Duplicate SKU code
     */
    public DuplicateInventoryException(String skuCode) {
        super(String.format("Inventory record for SKU code '%s' already exists", skuCode));
    }
}
