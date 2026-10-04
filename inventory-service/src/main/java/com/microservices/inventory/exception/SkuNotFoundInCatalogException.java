package com.microservices.inventory.exception;

/**
 * SKU Not Found In Catalog Exception
 *
 * Thrown when attempting to create an inventory record for a SKU that does
 * not exist in product-service's catalog
 *
 * @author Microservices Team
 * @version 1.0
 */
public class SkuNotFoundInCatalogException extends RuntimeException {

    /**
     * Constructs exception with SKU code
     *
     * @param skuCode SKU code that does not exist in the catalog
     */
    public SkuNotFoundInCatalogException(String skuCode) {
        super(String.format("Product with SKU code '%s' does not exist in the catalog", skuCode));
    }
}
