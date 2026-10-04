package com.microservices.inventory.repository;

import com.microservices.inventory.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Inventory Repository
 *
 * Provides data access operations for InventoryItem entity
 * Extends JpaRepository for CRUD operations
 *
 * @author Microservices Team
 * @version 1.0
 */
@Repository
public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {

    /**
     * Find inventory record by SKU code
     *
     * @param skuCode Stock Keeping Unit code
     * @return Optional containing the inventory record if found
     */
    Optional<InventoryItem> findBySkuCode(String skuCode);

    /**
     * Check if an inventory record exists for the given SKU code
     *
     * @param skuCode Stock Keeping Unit code
     * @return true if an inventory record exists, false otherwise
     */
    boolean existsBySkuCode(String skuCode);

    /**
     * Find inventory records for a batch of SKU codes
     *
     * @param skuCodes SKU codes to look up
     * @return inventory records found for the given SKU codes (unknown SKUs are silently omitted)
     */
    List<InventoryItem> findBySkuCodeIn(List<String> skuCodes);
}
