package com.microservices.product.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Product Highlight Entity - one short bullet point shown at the top of the detail page
 *
 * @author Microservices Team
 * @version 1.0
 */
@Entity
@Table(name = "product_highlights",
       indexes = @Index(name = "idx_product_highlights_product_id", columnList = "product_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ProductHighlight {

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

    @Column(name = "text", nullable = false, length = 255)
    private String text;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
