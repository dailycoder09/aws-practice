package com.microservices.product.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Inventory Client Implementation
 *
 * Calls inventory-service's GET /api/inventory/sku/{skuCode} endpoint using Spring 6.1's RestClient
 * to enrich a single-item product read with live stock info.
 *
 * Note on the asymmetry: this is intentionally the opposite failure-handling style from
 * inventory-service's own ProductClient (which throws on network/timeout/5xx, since that call
 * guards a write-path validation) - here, every failure (404, timeout, connection refused, 5xx,
 * malformed response) is swallowed into Optional.empty() because this is a read-path enrichment
 * call that must never break the primary product read.
 *
 * @author Microservices Team
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryClientImpl implements InventoryClient {

    private final RestClient inventoryRestClient;

    /**
     * Find current stock quantity for a given SKU code
     *
     * @param skuCode SKU code to look up
     * @return Optional containing quantity on hand, or empty if unknown/unavailable - never throws
     */
    @Override
    public Optional<Integer> findStockBySkuCode(String skuCode) {
        try {
            InventoryStockResponse stock = inventoryRestClient.get()
                    .uri("/api/inventory/sku/{skuCode}", skuCode)
                    .retrieve()
                    .body(InventoryStockResponse.class);

            return stock != null ? Optional.ofNullable(stock.quantityOnHand()) : Optional.empty();
        } catch (HttpClientErrorException.NotFound ex) {
            log.debug("No inventory record found for SKU: {}", skuCode);
            return Optional.empty();
        } catch (RestClientException ex) {
            log.warn("Inventory lookup failed for SKU: {} - {}", skuCode, ex.getMessage());
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("Unexpected error during inventory lookup for SKU: {} - {}", skuCode, ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Find current stock quantities for several SKU codes using inventory-service's batch endpoint
     *
     * @param skuCodes SKU codes to look up
     * @return Map of skuCode to quantity on hand, or an empty map if the list is empty or the
     *         lookup fails for any reason - never throws
     */
    @Override
    public Map<String, Integer> findStockForSkus(List<String> skuCodes) {
        if (skuCodes.isEmpty()) {
            return Map.of();
        }

        String joinedSkuCodes = String.join(",", skuCodes);

        // Same intentional asymmetry as findStockBySkuCode: every failure is swallowed into an empty map
        try {
            Map<String, Integer> stock = inventoryRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/inventory/batch")
                            .queryParam("skuCodes", joinedSkuCodes)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Integer>>() {
                    });

            return stock != null ? stock : Map.of();
        } catch (RestClientException ex) {
            log.warn("Batch inventory lookup failed for {} SKUs - {}", skuCodes.size(), ex.getMessage());
            return Map.of();
        } catch (Exception ex) {
            log.warn("Unexpected error during batch inventory lookup for {} SKUs - {}", skuCodes.size(), ex.getMessage());
            return Map.of();
        }
    }

    /**
     * Minimal local projection of inventory-service's response - intentionally not reusing
     * inventory-service's DTOs, since the two services are independently deployable and own
     * their own API contracts. Unknown fields (id, skuCode, reorderThreshold, timestamps, etc.)
     * are ignored; only quantityOnHand is needed here.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record InventoryStockResponse(Integer quantityOnHand) {
    }
}
