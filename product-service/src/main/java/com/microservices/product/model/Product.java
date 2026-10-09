package com.microservices.product.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Product Entity - Represents a product in the catalog
 *
 * This entity follows JPA best practices:
 * - Uses appropriate column definitions
 * - Implements proper constraints
 * - Uses Lombok to reduce boilerplate
 * - Implements audit fields (created_at, updated_at)
 *
 * The images/highlights/specifications collections are LAZY and excluded from toString;
 * list and search queries never touch them. Mutate them in place (clear/add) - never
 * replace the list instance, orphanRemoval would then fail.
 *
 * @author Microservices Team
 * @version 1.0
 */
@Entity
@Table(name = "products",
       indexes = {
           @Index(name = "idx_products_sku_code", columnList = "sku_code"),
           @Index(name = "idx_products_category", columnList = "category"),
           @Index(name = "idx_products_name", columnList = "name"),
           @Index(name = "idx_products_brand", columnList = "brand")
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
     * Brand name (optional)
     */
    @Column(name = "brand", length = 100)
    private String brand;

    /**
     * Maximum retail price (optional). The displayed discount is derived from mrp and
     * price on read and is never stored.
     */
    @Column(name = "mrp", precision = 10, scale = 2)
    private BigDecimal mrp;

    /**
     * Warranty description (optional)
     */
    @Column(name = "warranty", length = 100)
    private String warranty;

    /**
     * Seller name (optional)
     */
    @Column(name = "seller", length = 100)
    private String seller;

    /**
     * Denormalised primary image URL (the sort_order = 1 row of product_images, or null),
     * so list queries never need the images table
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * Product images, ordered by position
     */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @ToString.Exclude
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    /**
     * Product highlights (bullet points), ordered by position
     */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @ToString.Exclude
    @Builder.Default
    private List<ProductHighlight> highlights = new ArrayList<>();

    /**
     * Product specification rows, ordered by position
     */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @ToString.Exclude
    @Builder.Default
    private List<ProductSpecification> specifications = new ArrayList<>();

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
