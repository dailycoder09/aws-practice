package com.microservices.product.client;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Inventory Client Interface
 *
 * Looks up live stock levels from inventory-service to enrich single-item product reads
 * and the server-rendered catalog page
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

    /**
     * Find current stock quantities for several SKU codes in a single call
     * (avoids N+1 per-SKU lookups when rendering a page of products)
     *
     * @param skuCodes SKU codes to look up
     * @return Map of skuCode to quantity on hand; SKUs that are unknown to inventory-service are
     *         absent, and the map is empty if inventory-service is unavailable
     */
    Map<String, Integer> findStockForSkus(List<String> skuCodes);
}
