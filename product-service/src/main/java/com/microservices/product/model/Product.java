package com.microservices.product.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Product Entity - Represents a product in the catalog
 * 
 * This entity follows JPA best practices:
 * - Uses appropriate column definitions
 * - Implements proper constraints
 * - Uses Lombok to reduce boilerplate
 * - Implements audit fields (created_at, updated_at)
 * 
 * @author Microservices Team
 * @version 1.0
 */
@Entity
@Table(name = "products", 
       indexes = {
           @Index(name = "idx_products_sku_code", columnList = "sku_code"),
           @Index(name = "idx_products_category", columnList = "category"),
           @Index(name = "idx_products_name", columnList = "name")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "id")
public class Product {

    /**
     * Unique identifier for the product
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Product name - required field
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * Detailed product description
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Product price in USD
     * Using BigDecimal for precise decimal calculations
     */
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Product category for classification
     */
    @Column(name = "category", length = 100)
    private String category;

    /**
     * Stock Keeping Unit - unique identifier for inventory management
     */
    @Column(name = "sku_code", nullable = false, unique = true, length = 50)
    private String skuCode;

    /**
     * Timestamp when the product was created
     * Automatically populated by Hibernate
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the product was last updated
     * Automatically updated by Hibernate on each modification
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Pre-persist callback to validate entity before saving
     */
    @PrePersist
    @PreUpdate
    public void validateEntity() {
        if (name != null) {
            name = name.trim();
        }
        if (skuCode != null) {
            skuCode = skuCode.trim().toUpperCase();
        }
        if (category != null) {
            category = category.trim();
        }
    }
}
