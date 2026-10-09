package com.microservices.product.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Product Response DTO
 * 
 * Used for returning product information to clients
 * Separates internal entity structure from external API contract
 * 
 * @author Microservices Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    /**
     * Unique product identifier
     */
    private Long id;

    /**
     * Product name
     */
    private String name;

    /**
     * Product description
     */
    private String description;

    /**
     * Product price
     */
    private BigDecimal price;

    /**
     * Product category
     */
    private String category;

    /**
     * Stock Keeping Unit code
     */
    private String skuCode;

    /**
     * Brand name (nullable)
     */
    private String brand;

    /**
     * Maximum retail price (nullable)
     */
    private BigDecimal mrp;

    /**
     * Derived round((mrp - price) / mrp * 100); null when mrp is null or not above price
     */
    private Integer discountPercent;

    /**
     * Primary image URL (nullable); the full image list is only on the detail endpoints
     */
    private String imageUrl;

    /**
     * Creation timestamp
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * Last update timestamp
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * Live stock quantity from inventory-service, populated only on single-item reads.
     * Null when inventory-service has no record for this SKU or is unavailable.
     * Always serialized (even when null) so API consumers can distinguish "no stock info yet"
     * from the field not existing, overriding the app-wide non_null Jackson default just here.
     */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private Integer stockQuantity;
}
