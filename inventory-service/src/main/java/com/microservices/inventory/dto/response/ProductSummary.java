package com.microservices.inventory.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Product Summary DTO
 *
 * A minimal shape used to deserialize product-service's ProductResponse when
 * ProductClient validates a SKU exists in the catalog. Only the fields
 * inventory-service actually needs are declared here; {@code @JsonIgnoreProperties}
 * tells Jackson to silently ignore the extra fields product-service actually
 * returns (description, category, createdAt, updatedAt, ...) instead of
 * failing deserialization.
 *
 * @author Microservices Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductSummary {

    /**
     * SKU Code
     */
    private String skuCode;

    /**
     * Product name
     */
    private String name;

    /**
     * Product price
     */
    private BigDecimal price;
}
