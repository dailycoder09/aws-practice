package com.microservices.inventory.controller;

import com.microservices.inventory.dto.request.InventoryRequest;
import com.microservices.inventory.dto.response.InventoryAuditLogResponse;
import com.microservices.inventory.dto.response.InventoryResponse;
import com.microservices.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Inventory REST Controller
 *
 * Provides RESTful API endpoints for Inventory management
 * Follows REST best practices with proper HTTP methods and status codes
 * Comprehensive API documentation with Swagger/OpenAPI annotations
 *
 * @author Microservices Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Inventory Management", description = "APIs for managing stock levels and audit history")
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * Create a new inventory record
     *
     * @param request Inventory creation request
     * @return Created inventory response with HTTP 201
     */
    @PostMapping
    @Operation(summary = "Create a new inventory record", description = "Creates a stock record for a SKU that exists in product-service's catalog")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Inventory record created successfully",
                    content = @Content(schema = @Schema(implementation = InventoryResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "409", description = "Inventory record for SKU already exists"),
        @ApiResponse(responseCode = "422", description = "SKU does not exist in product-service's catalog"),
        @ApiResponse(responseCode = "503", description = "Product catalog service is unavailable")
    })
    public ResponseEntity<InventoryResponse> createInventory(
            @Valid @RequestBody InventoryRequest request) {
        log.info("REST request to create inventory record for SKU: {}", request.getSkuCode());

        InventoryResponse response = inventoryService.createInventory(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get inventory record by ID
     *
     * @param id Inventory record ID
     * @return Inventory response with HTTP 200
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get inventory record by ID", description = "Retrieves an inventory record by its unique identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Inventory record found",
                    content = @Content(schema = @Schema(implementation = InventoryResponse.class))),
        @ApiResponse(responseCode = "404", description = "Inventory record not found")
    })
    public ResponseEntity<InventoryResponse> getById(
            @Parameter(description = "Inventory record ID") @PathVariable Long id) {
        log.debug("REST request to get inventory record with ID: {}", id);

        InventoryResponse response = inventoryService.getById(id);

        return ResponseEntity.ok(response);
    }

    /**
     * Get inventory record by SKU code
     *
     * @param skuCode Stock Keeping Unit code
     * @return Inventory response with HTTP 200
     */
    @GetMapping("/sku/{skuCode}")
    @Operation(summary = "Get inventory record by SKU", description = "Retrieves an inventory record by its SKU code - this is what product-service calls for stock enrichment")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Inventory record found",
                    content = @Content(schema = @Schema(implementation = InventoryResponse.class))),
        @ApiResponse(responseCode = "404", description = "Inventory record not found")
    })
    public ResponseEntity<InventoryResponse> getBySkuCode(
            @Parameter(description = "SKU Code") @PathVariable String skuCode) {
        log.debug("REST request to get inventory record with SKU: {}", skuCode);

        InventoryResponse response = inventoryService.getBySkuCode(skuCode);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all inventory records with pagination
     *
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @param sortBy Sort field (default: id)
     * @param sortDir Sort direction (default: asc)
     * @return Page of inventory records with HTTP 200
     */
    @GetMapping
    @Operation(summary = "Get all inventory records", description = "Retrieves all inventory records with pagination and sorting")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Inventory records retrieved successfully")
    })
    public ResponseEntity<Page<InventoryResponse>> getAll(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "asc") String sortDir) {
        log.debug("REST request to get all inventory records - Page: {}, Size: {}, Sort: {} {}",
                  page, size, sortBy, sortDir);

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<InventoryResponse> inventory = inventoryService.getAll(pageable);

        return ResponseEntity.ok(inventory);
    }

    /**
     * Update an existing inventory record's quantity/threshold
     *
     * @param id Inventory record ID
     * @param request Inventory update request
     * @return Updated inventory response with HTTP 200
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update inventory record", description = "Replaces an inventory record's quantity and reorder threshold")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Inventory record updated successfully",
                    content = @Content(schema = @Schema(implementation = InventoryResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "Inventory record not found")
    })
    public ResponseEntity<InventoryResponse> update(
            @Parameter(description = "Inventory record ID") @PathVariable Long id,
            @Valid @RequestBody InventoryRequest request) {
        log.info("REST request to update inventory record with ID: {}", id);

        InventoryResponse response = inventoryService.update(id, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Increment or decrement a SKU's quantity on hand
     *
     * @param skuCode Stock Keeping Unit code
     * @param delta Amount to add (or, if negative, subtract)
     * @return Updated inventory response with HTTP 200
     */
    @PatchMapping("/sku/{skuCode}/adjust")
    @Operation(summary = "Adjust stock", description = "Increments or decrements a SKU's quantity on hand")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stock adjusted successfully",
                    content = @Content(schema = @Schema(implementation = InventoryResponse.class))),
        @ApiResponse(responseCode = "400", description = "Resulting quantity would be negative"),
        @ApiResponse(responseCode = "404", description = "Inventory record not found")
    })
    public ResponseEntity<InventoryResponse> adjustStock(
            @Parameter(description = "SKU Code") @PathVariable String skuCode,
            @Parameter(description = "Amount to add, or subtract if negative") @RequestParam int delta) {
        log.info("REST request to adjust stock for SKU: {} by delta: {}", skuCode, delta);

        InventoryResponse response = inventoryService.adjustStock(skuCode, delta);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete inventory record by ID
     *
     * @param id Inventory record ID
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete inventory record", description = "Deletes an inventory record by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Inventory record deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Inventory record not found")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Inventory record ID") @PathVariable Long id) {
        log.info("REST request to delete inventory record with ID: {}", id);

        inventoryService.delete(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Get paginated audit history for a SKU, most recent first
     *
     * @param skuCode Stock Keeping Unit code
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @return Page of audit log entries with HTTP 200
     */
    @GetMapping("/sku/{skuCode}/audit")
    @Operation(summary = "Get audit history for a SKU", description = "Retrieves paginated stock-change history for a SKU, most recent first")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Audit history retrieved successfully")
    })
    public ResponseEntity<Page<InventoryAuditLogResponse>> getAuditHistory(
            @Parameter(description = "SKU Code") @PathVariable String skuCode,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        log.debug("REST request to get audit history for SKU: {}", skuCode);

        Pageable pageable = PageRequest.of(page, size);
        Page<InventoryAuditLogResponse> history = inventoryService.getAuditHistory(skuCode, pageable);

        return ResponseEntity.ok(history);
    }

    /**
     * Get quantity on hand for a batch of SKU codes in a single call
     *
     * @param skuCodes Comma-separated SKU codes to look up
     * @return Map of skuCode to quantityOnHand with HTTP 200, containing only the SKUs that were found
     */
    @GetMapping("/batch")
    @Operation(summary = "Get stock levels for a batch of SKUs", description = "Retrieves quantity on hand for multiple SKUs in one call, so a UI page showing many products doesn't need one request per product")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stock levels retrieved successfully (unknown SKUs are silently omitted)")
    })
    public ResponseEntity<Map<String, Integer>> getBatchStock(
            @Parameter(description = "Comma-separated SKU codes") @RequestParam(required = false, defaultValue = "") List<String> skuCodes) {
        log.debug("REST request to get batch stock levels for {} SKU(s)", skuCodes.size());

        if (skuCodes.isEmpty()) {
            return ResponseEntity.ok(Map.of());
        }

        Map<String, Integer> stock = inventoryService.getStockForSkus(skuCodes);

        return ResponseEntity.ok(stock);
    }
}
