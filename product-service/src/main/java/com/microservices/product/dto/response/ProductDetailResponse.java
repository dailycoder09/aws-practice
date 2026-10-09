package com.microservices.product.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Product Detail Response DTO
 *
 * Full "product detail page" payload: scalar fields plus images, highlights and
 * specifications grouped by group name. Only returned by the single-item
 * detail endpoints - list/search responses use the lighter {@link ProductResponse}.
 *
 * @author Microservices Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetailResponse {

    private Long id;

    private String name;

    private String description;

    private BigDecimal price;

    /**
     * Maximum retail price; null when not set
     */
    private BigDecimal mrp;

    /**
     * Derived round((mrp - price) / mrp * 100); null when mrp is null or not above price
     */
    private Integer discountPercent;

    private String category;

    private String brand;

    private String skuCode;

    private String warranty;

    private String seller;

    /**
     * Live stock quantity from inventory-service; null when unknown/unavailable (fail-open).
     * Always serialized (even when null), same treatment as {@link ProductResponse#getStockQuantity()}.
     */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private Integer stockQuantity;

    /**
     * Images in display order
     */
    private List<Image> images;

    /**
     * Highlight bullet points in display order
     */
    private List<String> highlights;

    /**
     * Specifications grouped by group name: groups in order of first appearance,
     * items in sort order
     */
    private List<SpecificationGroup> specifications;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * One product image
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Image {
        private String url;
        private String alt;
    }

    /**
     * One group of specification rows
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SpecificationGroup {
        private String group;
        private List<SpecificationItem> items;
    }

    /**
     * One specification key/value row
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SpecificationItem {
        private String key;
        private String value;
    }
}
