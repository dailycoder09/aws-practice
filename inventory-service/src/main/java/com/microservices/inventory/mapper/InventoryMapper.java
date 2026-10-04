package com.microservices.inventory.mapper;

import com.microservices.inventory.dto.response.InventoryAuditLogResponse;
import com.microservices.inventory.dto.response.InventoryResponse;
import com.microservices.inventory.model.InventoryAuditLog;
import com.microservices.inventory.model.InventoryItem;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * Inventory Mapper Interface
 *
 * Uses MapStruct for automatic mapping between Entity and DTOs
 * Provides compile-time type-safe mapping with better performance than reflection-based mappers
 *
 * @author Microservices Team
 * @version 1.0
 */
@Mapper(componentModel = "spring")
public interface InventoryMapper {

    /**
     * Maps InventoryItem entity to InventoryResponse DTO
     *
     * @param inventoryItem InventoryItem entity
     * @return Inventory response DTO
     */
    InventoryResponse toResponse(InventoryItem inventoryItem);

    /**
     * Maps InventoryAuditLog entity to InventoryAuditLogResponse DTO
     *
     * @param auditLog InventoryAuditLog entity
     * @return Inventory audit log response DTO
     */
    InventoryAuditLogResponse toAuditResponse(InventoryAuditLog auditLog);

    /**
     * Maps list of InventoryAuditLog entities to list of InventoryAuditLogResponse DTOs
     *
     * @param auditLogs List of audit log entities
     * @return List of audit log response DTOs
     */
    List<InventoryAuditLogResponse> toAuditResponseList(List<InventoryAuditLog> auditLogs);
}
