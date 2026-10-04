package com.microservices.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Adjust Stock Request DTO
 *
 * Used to increment or decrement the quantity on hand for a SKU.
 * delta may be negative (decrement) or positive (increment).
 *
 * @author Microservices Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdjustStockRequest {

    /**
     * Amount to add to (or, if negative, subtract from) the current quantity on hand
     */
    @NotNull(message = "Delta is required")
    private Integer delta;
}
