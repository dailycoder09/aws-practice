package com.microservices.product.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Product Specification Entity - one key/value row of the specification table
 *
 * Rows are grouped by {@code groupName} when building the API response; the group
 * order is the order in which each group first appears by {@code sortOrder}.
 *
 * @author Microservices Team
 * @version 1.0
 */
@Entity
@Table(name = "product_specifications",
       indexes = @Index(name = "idx_product_specifications_product_id", columnList = "product_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ProductSpecification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Owning product (excluded from toString so logging never lazy-loads or recurses)
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    private Product product;

    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

    @Column(name = "spec_key", nullable = false, length = 100)
    private String specKey;

    @Column(name = "spec_value", nullable = false, length = 255)
    private String specValue;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
