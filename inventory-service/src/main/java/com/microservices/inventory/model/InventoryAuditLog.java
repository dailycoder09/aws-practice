package com.microservices.inventory.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * InventoryAuditLog Entity - An immutable record of a single stock change
 *
 * Append-only: rows in this table are never updated or deleted. Every
 * create/adjust/update/delete on an InventoryItem writes exactly one row
 * here, inside the same transaction boundary as the InventoryItem write it
 * describes, so the two can never drift apart.
 *
 * @author Microservices Team
 * @version 1.0
 */
@Entity
@Table(name = "inventory_audit_log",
       indexes = {
           @Index(name = "idx_inventory_audit_log_sku_code", columnList = "sku_code")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "id")
public class InventoryAuditLog {

    /**
     * Unique identifier for the audit log entry
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * SKU code the change applies to
     */
    @Column(name = "sku_code", nullable = false, length = 50)
    private String skuCode;

    /**
     * The kind of change this entry records
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 20)
    private ChangeType changeType;

    /**
     * Quantity before the change - null for CREATED
     */
    @Column(name = "previous_quantity")
    private Integer previousQuantity;

    /**
     * Quantity after the change - null for DELETED
     */
    @Column(name = "new_quantity")
    private Integer newQuantity;

    /**
     * Timestamp when this change occurred
     * Automatically populated by Hibernate
     */
    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;
}
