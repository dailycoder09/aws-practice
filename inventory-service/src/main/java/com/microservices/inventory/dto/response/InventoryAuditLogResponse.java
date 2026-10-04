package com.microservices.inventory.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Inventory Audit Log Response DTO
 *
 * Used for returning a single stock-change history entry to clients
 *
 * @author Microservices Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryAuditLogResponse {

    /**
     * Unique audit log entry identifier
     */
    private Long id;

    /**
     * SKU Code the change applies to
     */
    private String skuCode;

    /**
     * The kind of change this entry records (CREATED/ADJUSTED/UPDATED/DELETED)
     */
    private String changeType;

    /**
     * Quantity before the change - null for CREATED
     */
    private Integer previousQuantity;

    /**
     * Quantity after the change - null for DELETED
     */
    private Integer newQuantity;

    /**
     * Timestamp when this change occurred
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime changedAt;
}
