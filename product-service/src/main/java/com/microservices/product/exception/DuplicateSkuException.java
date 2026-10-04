package com.microservices.product.exception;

/**
 * Duplicate SKU Exception
 * 
 * Thrown when attempting to create a product with an already existing SKU code
 * 
 * @author Microservices Team
 * @version 1.0
 */
public class DuplicateSkuException extends RuntimeException {

    /**
     * Constructs exception with SKU code
     * 
     * @param skuCode Duplicate SKU code
     */
    public DuplicateSkuException(String skuCode) {
        super(String.format("Product with SKU code '%s' already exists", skuCode));
    }
}
