package com.microservices.inventory.service.impl;

import com.microservices.inventory.client.ProductClient;
import com.microservices.inventory.dto.request.InventoryRequest;
import com.microservices.inventory.dto.response.InventoryAuditLogResponse;
import com.microservices.inventory.dto.response.InventoryResponse;
import com.microservices.inventory.exception.DuplicateInventoryException;
import com.microservices.inventory.exception.InventoryNotFoundException;
import com.microservices.inventory.exception.SkuNotFoundInCatalogException;
import com.microservices.inventory.mapper.InventoryMapper;
import com.microservices.inventory.model.ChangeType;
import com.microservices.inventory.model.InventoryAuditLog;
import com.microservices.inventory.model.InventoryItem;
import com.microservices.inventory.repository.InventoryAuditLogRepository;
import com.microservices.inventory.repository.InventoryRepository;
import com.microservices.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Inventory Service Implementation
 *
 * Implements business logic for Inventory management
 * Uses @Transactional for database transaction management
 * Comprehensive logging for monitoring and debugging
 *
 * @author Microservices Team
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryAuditLogRepository inventoryAuditLogRepository;
    private final InventoryMapper inventoryMapper;
    private final ProductClient productClient;

    /**
     * Create a new inventory record
     * Validates the SKU exists in product-service's catalog and is not already tracked
     */
    @Override
    @Transactional
    public InventoryResponse createInventory(InventoryRequest request) {
        log.info("Creating new inventory record for SKU: {}", request.getSkuCode());

        if (productClient.findBySkuCode(request.getSkuCode()).isEmpty()) {
            log.warn("Cannot create inventory - SKU '{}' does not exist in catalog", request.getSkuCode());
            throw new SkuNotFoundInCatalogException(request.getSkuCode());
        }

        if (inventoryRepository.existsBySkuCode(request.getSkuCode())) {
            log.warn("Attempt to create inventory for duplicate SKU: {}", request.getSkuCode());
            throw new DuplicateInventoryException(request.getSkuCode());
        }

        InventoryItem inventoryItem = InventoryItem.builder()
                .skuCode(request.getSkuCode())
                .quantityOnHand(request.getQuantityOnHand())
                .reorderThreshold(request.getReorderThreshold() != null ? request.getReorderThreshold() : 10)
                .build();

        InventoryItem savedItem = inventoryRepository.save(inventoryItem);

        writeAuditLog(savedItem.getSkuCode(), ChangeType.CREATED, null, savedItem.getQuantityOnHand());

        log.info("Successfully created inventory record with ID: {} and SKU: {}",
                 savedItem.getId(), savedItem.getSkuCode());

        return inventoryMapper.toResponse(savedItem);
    }

    /**
     * Get inventory record by ID
     */
    @Override
    public InventoryResponse getById(Long id) {
        log.debug("Fetching inventory record with ID: {}", id);

        InventoryItem inventoryItem = inventoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Inventory record not found with ID: {}", id);
                    return new InventoryNotFoundException(id);
                });

        return inventoryMapper.toResponse(inventoryItem);
    }

    /**
     * Get inventory record by SKU code
     */
    @Override
    public InventoryResponse getBySkuCode(String skuCode) {
        log.debug("Fetching inventory record with SKU: {}", skuCode);

        InventoryItem inventoryItem = inventoryRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> {
                    log.error("Inventory record not found with SKU: {}", skuCode);
                    return new InventoryNotFoundException(skuCode);
                });

        return inventoryMapper.toResponse(inventoryItem);
    }

    /**
     * Get all inventory records with pagination
     */
    @Override
    public Page<InventoryResponse> getAll(Pageable pageable) {
        log.debug("Fetching all inventory records - Page: {}, Size: {}",
                  pageable.getPageNumber(), pageable.getPageSize());

        return inventoryRepository.findAll(pageable).map(inventoryMapper::toResponse);
    }

    /**
     * Update an existing inventory record's quantity/threshold
     */
    @Override
    @Transactional
    public InventoryResponse update(Long id, InventoryRequest request) {
        log.info("Updating inventory record with ID: {}", id);

        InventoryItem existingItem = inventoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Cannot update - inventory record not found with ID: {}", id);
                    return new InventoryNotFoundException(id);
                });

        Integer previousQuantity = existingItem.getQuantityOnHand();

        existingItem.setQuantityOnHand(request.getQuantityOnHand());
        if (request.getReorderThreshold() != null) {
            existingItem.setReorderThreshold(request.getReorderThreshold());
        }

        InventoryItem updatedItem = inventoryRepository.save(existingItem);

        writeAuditLog(updatedItem.getSkuCode(), ChangeType.UPDATED, previousQuantity, updatedItem.getQuantityOnHand());

        log.info("Successfully updated inventory record with ID: {}", id);

        return inventoryMapper.toResponse(updatedItem);
    }

    /**
     * Increment or decrement a SKU's quantity on hand
     */
    @Override
    @Transactional
    public InventoryResponse adjustStock(String skuCode, int delta) {
        log.info("Adjusting stock for SKU: {} by delta: {}", skuCode, delta);

        InventoryItem inventoryItem = inventoryRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> {
                    log.error("Cannot adjust stock - inventory record not found with SKU: {}", skuCode);
                    return new InventoryNotFoundException(skuCode);
                });

        int previousQuantity = inventoryItem.getQuantityOnHand();
        int newQuantity = previousQuantity + delta;

        if (newQuantity < 0) {
            log.warn("Rejecting stock adjustment for SKU '{}': {} + {} would go negative", skuCode, previousQuantity, delta);
            throw new IllegalArgumentException("Resulting quantity cannot be negative");
        }

        inventoryItem.setQuantityOnHand(newQuantity);
        InventoryItem savedItem = inventoryRepository.save(inventoryItem);

        writeAuditLog(savedItem.getSkuCode(), ChangeType.ADJUSTED, previousQuantity, newQuantity);

        log.info("Successfully adjusted stock for SKU '{}': {} -> {}", skuCode, previousQuantity, newQuantity);

        return inventoryMapper.toResponse(savedItem);
    }

    /**
     * Delete an inventory record by ID
     */
    @Override
    @Transactional
    public void delete(Long id) {
        log.info("Deleting inventory record with ID: {}", id);

        InventoryItem inventoryItem = inventoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Cannot delete - inventory record not found with ID: {}", id);
                    return new InventoryNotFoundException(id);
                });

        Integer currentQuantity = inventoryItem.getQuantityOnHand();
        String skuCode = inventoryItem.getSkuCode();

        inventoryRepository.delete(inventoryItem);

        writeAuditLog(skuCode, ChangeType.DELETED, currentQuantity, null);

        log.info("Successfully deleted inventory record with ID: {}", id);
    }

    /**
     * Get paginated audit history for a SKU, most recent first
     */
    @Override
    public Page<InventoryAuditLogResponse> getAuditHistory(String skuCode, Pageable pageable) {
        log.debug("Fetching audit history for SKU: {} - Page: {}, Size: {}",
                  skuCode, pageable.getPageNumber(), pageable.getPageSize());

        return inventoryAuditLogRepository.findBySkuCodeOrderByChangedAtDesc(skuCode, pageable)
                .map(inventoryMapper::toAuditResponse);
    }

    /**
     * Get quantity on hand for a batch of SKU codes in a single call
     */
    @Override
    public Map<String, Integer> getStockForSkus(List<String> skuCodes) {
        log.debug("Fetching batch stock levels for {} SKU(s)", skuCodes.size());

        if (skuCodes.isEmpty()) {
            return Map.of();
        }

        return inventoryRepository.findBySkuCodeIn(skuCodes).stream()
                .collect(Collectors.toMap(InventoryItem::getSkuCode, InventoryItem::getQuantityOnHand));
    }

    private void writeAuditLog(String skuCode, ChangeType changeType, Integer previousQuantity, Integer newQuantity) {
        InventoryAuditLog auditLog = InventoryAuditLog.builder()
                .skuCode(skuCode)
                .changeType(changeType)
                .previousQuantity(previousQuantity)
                .newQuantity(newQuantity)
                .build();

        inventoryAuditLogRepository.save(auditLog);
    }
}
