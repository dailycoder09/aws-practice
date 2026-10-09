package com.microservices.product.client;

import java.util.Optional;

/**
 * Inventory Client Interface
 *
 * Looks up live stock levels from inventory-service to enrich single-item product reads
 *
 * @author Microservices Team
 * @version 1.0
 */
public interface InventoryClient {

    /**
     * Find current stock quantity for a given SKU code
     *
     * @param skuCode SKU code to look up
     * @return Optional containing quantity on hand, or empty if unknown/unavailable
     */
    Optional<Integer> findStockBySkuCode(String skuCode);
}
