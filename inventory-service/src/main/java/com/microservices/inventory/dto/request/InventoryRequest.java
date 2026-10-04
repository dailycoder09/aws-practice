package com.microservices.inventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Inventory Request DTO
 *
 * Used for creating and updating inventory records
 * Includes Bean Validation annotations for input validation
 *
 * @author Microservices Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryRequest {

    /**
     * SKU Code - required, must exist in product-service's catalog
     */
    @NotBlank(message = "SKU code is required")
    private String skuCode;

    /**
     * Quantity on hand - required, must not be negative
     */
    @NotNull(message = "Quantity on hand is required")
    @Min(value = 0, message = "Quantity on hand must not be negative")
    private Integer quantityOnHand;

    /**
     * Reorder threshold - optional, defaults to 10 when not supplied
     */
    private Integer reorderThreshold;
}
