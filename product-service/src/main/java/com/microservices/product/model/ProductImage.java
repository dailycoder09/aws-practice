package com.microservices.product.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Product Image Entity - one image URL of a product
 *
 * Only a URL is stored (no upload, no file storage). Position is {@code sortOrder}
 * (1-based, unique per product); the row with sortOrder 1 is the primary image that is
 * mirrored into {@link Product#getImageUrl()}.
 *
 * Identity equality is deliberate (no equals/hashCode on id): children are created
 * transient with a null id and live in a list owned by {@link Product}.
 *
 * @author Microservices Team
 * @version 1.0
 */
@Entity
@Table(name = "product_images",
       indexes = @Index(name = "idx_product_images_product_id", columnList = "product_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ProductImage {

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

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "alt", length = 255)
    private String alt;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
