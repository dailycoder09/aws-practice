package com.microservices.inventory.repository;

import com.microservices.inventory.model.InventoryAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Inventory Audit Log Repository
 *
 * Provides data access operations for InventoryAuditLog entity
 * Extends JpaRepository for CRUD operations
 *
 * @author Microservices Team
 * @version 1.0
 */
@Repository
public interface InventoryAuditLogRepository extends JpaRepository<InventoryAuditLog, Long> {

    /**
     * Find audit history for a SKU, most recent first
     *
     * @param skuCode Stock Keeping Unit code
     * @param pageable Pagination information
     * @return Page of audit log entries for the given SKU, newest first
     */
    Page<InventoryAuditLog> findBySkuCodeOrderByChangedAtDesc(String skuCode, Pageable pageable);
}
