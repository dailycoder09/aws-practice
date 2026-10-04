package com.microservices.inventory.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Inventory Response DTO
 *
 * Used for returning inventory information to clients
 * Separates internal entity structure from external API contract
 *
 * @author Microservices Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryResponse {

    /**
     * Unique inventory record identifier
     */
    private Long id;

    /**
     * SKU Code
     */
    private String skuCode;

    /**
     * Current quantity on hand
     */
    private Integer quantityOnHand;

    /**
     * Threshold below which this SKU should be reordered
     */
    private Integer reorderThreshold;

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
}
