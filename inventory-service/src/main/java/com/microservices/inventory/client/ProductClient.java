package com.microservices.inventory.client;

import com.microservices.inventory.dto.response.ProductSummary;

import java.util.Optional;

/**
 * Product Client Interface
 *
 * Defines the contract for calling product-service to validate that a SKU
 * exists in the catalog. "Exists" is simply {@code findBySkuCode(sku).isPresent()}.
 *
 * @author Microservices Team
 * @version 1.0
 */
public interface ProductClient {

    /**
     * Look up a product by SKU code in product-service's catalog
     *
     * @param skuCode Stock Keeping Unit code
     * @return Optional containing the product summary if found, empty if product-service
     *         reports the SKU does not exist (404)
     * @throws com.microservices.inventory.exception.ProductServiceUnavailableException
     *         if product-service is unreachable, times out, or returns any other error
     */
    Optional<ProductSummary> findBySkuCode(String skuCode);
}
