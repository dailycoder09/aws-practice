package com.microservices.inventory.client;

import com.microservices.inventory.dto.response.ProductSummary;
import com.microservices.inventory.exception.ProductServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

/**
 * Product Client Implementation
 *
 * RestClient-based implementation that calls GET /api/products/sku/{skuCode}
 * on product-service.
 *
 * Note on the asymmetry: this client throws {@link ProductServiceUnavailableException}
 * on any failure other than a 404 (connection refused, timeout, 5xx, other 4xx)
 * because it backs a write-path validation (POST /api/inventory) - if we can't
 * confirm the SKU is real, we must fail loudly rather than silently accept bad
 * data. This is the opposite of product-service's InventoryClient, which
 * swallows the same failures into Optional.empty() because it backs a
 * read-path enrichment that must never break the primary product read. Both
 * are intentional, not inconsistent - see the design spec's "Note on the
 * asymmetry".
 *
 * @author Microservices Team
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductClientImpl implements ProductClient {

    private final RestClient productServiceRestClient;

    @Override
    public Optional<ProductSummary> findBySkuCode(String skuCode) {
        try {
            // retrieve() throws HttpClientErrorException/HttpServerErrorException on
            // non-2xx status by default, and ResourceAccessException (a RestClientException)
            // on connection failures/timeouts - we rely on that default behavior here.
            ProductSummary product = productServiceRestClient.get()
                    .uri("/api/products/sku/{skuCode}", skuCode)
                    .retrieve()
                    .body(ProductSummary.class);

            return Optional.ofNullable(product);
        } catch (HttpClientErrorException.NotFound ex) {
            log.debug("Product with SKU '{}' not found in catalog (404)", skuCode);
            return Optional.empty();
        } catch (RestClientException ex) {
            log.error("Failed to reach product-service for SKU '{}': {}", skuCode, ex.getMessage());
            throw new ProductServiceUnavailableException(
                    "Product catalog service is currently unavailable", ex);
        }
    }
}
