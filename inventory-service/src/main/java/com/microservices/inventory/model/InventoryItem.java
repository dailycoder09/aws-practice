package com.microservices.inventory.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * InventoryItem Entity - Represents a stock record for a single SKU
 *
 * sku_code is NOT a database-level foreign key to product-service's products
 * table - each service owns its own database. Validity is enforced via the
 * REST call to product-service (see ProductClient) at write time, not SQL.
 *
 * @author Microservices Team
 * @version 1.0
 */
@Entity
@Table(name = "inventory_items",
       indexes = {
           @Index(name = "idx_inventory_items_sku_code", columnList = "sku_code")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "id")
public class InventoryItem {

    /**
     * Unique identifier for the inventory record
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Stock Keeping Unit - unique identifier matching product-service's catalog
     */
    @Column(name = "sku_code", nullable = false, unique = true, length = 50)
    private String skuCode;

    /**
     * Current quantity on hand - must never go negative
     */
    @Column(name = "quantity_on_hand", nullable = false)
    private Integer quantityOnHand;

    /**
     * Threshold below which this SKU should be reordered
     */
    @Column(name = "reorder_threshold", nullable = false)
    @Builder.Default
    private Integer reorderThreshold = 10;

    /**
     * Timestamp when the inventory record was created
     * Automatically populated by Hibernate
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the inventory record was last updated
     * Automatically updated by Hibernate on each modification
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
