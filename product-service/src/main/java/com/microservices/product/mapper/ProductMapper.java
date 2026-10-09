package com.microservices.product.mapper;

import com.microservices.product.dto.request.ProductRequest;
import com.microservices.product.dto.response.ProductDetailResponse;
import com.microservices.product.dto.response.ProductResponse;
import com.microservices.product.model.Product;
import com.microservices.product.model.ProductHighlight;
import com.microservices.product.model.ProductImage;
import com.microservices.product.model.ProductSpecification;
import org.mapstruct.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Product Mapper Interface
 *
 * Uses MapStruct for automatic mapping between Entity and DTOs
 * Provides compile-time type-safe mapping with better performance than reflection-based mappers
 *
 * @author Microservices Team
 * @version 1.0
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    imports = DiscountCalculator.class
)
public interface ProductMapper {

    /**
     * Maps ProductRequest DTO to Product entity
     *
     * The highlights/specifications lists in the request have a different element type than the
     * entity's child collections, so they are ignored here and handled explicitly by the service.
     * imageUrl and images are never set from a product request (images have their own endpoint).
     *
     * @param request Product request DTO
     * @return Product entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "highlights", ignore = true)
    @Mapping(target = "specifications", ignore = true)
    Product toEntity(ProductRequest request);

    /**
     * Maps Product entity to the light ProductResponse DTO used by list/search/CRUD
     *
     * Maps scalar fields only (plus the derived discountPercent and the denormalised imageUrl);
     * it must never touch the lazy images/highlights/specifications collections.
     *
     * @param product Product entity
     * @return Product response DTO
     */
    @Mapping(target = "discountPercent",
             expression = "java(DiscountCalculator.discountPercent(product.getMrp(), product.getPrice()))")
    ProductResponse toResponse(Product product);

    /**
     * Maps list of Product entities to list of ProductResponse DTOs
     *
     * @param products List of product entities
     * @return List of product response DTOs
     */
    List<ProductResponse> toResponseList(List<Product> products);

    /**
     * Maps Product entity to the full ProductDetailResponse DTO (images, highlights, grouped
     * specifications). Initialises the lazy collections, so call it inside a transaction.
     * stockQuantity is not mapped; the service fills it from inventory-service.
     *
     * @param product Product entity
     * @return Product detail response DTO
     */
    @Mapping(target = "discountPercent",
             expression = "java(DiscountCalculator.discountPercent(product.getMrp(), product.getPrice()))")
    @Mapping(target = "images", qualifiedByName = "imageResponses")
    @Mapping(target = "highlights", qualifiedByName = "highlightTexts")
    @Mapping(target = "specifications", qualifiedByName = "groupSpecifications")
    @Mapping(target = "stockQuantity", ignore = true)
    ProductDetailResponse toDetailResponse(Product product);

    /**
     * Updates existing Product entity with data from ProductRequest
     * Null values in the request will be ignored (won't update existing values)
     *
     * @param request Product request DTO with updated data
     * @param product Existing product entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "highlights", ignore = true)
    @Mapping(target = "specifications", ignore = true)
    void updateEntityFromRequest(ProductRequest request, @MappingTarget Product product);

    /**
     * Maps one image entity to its response form (url + alt)
     */
    ProductDetailResponse.Image toImageResponse(ProductImage image);

    /**
     * Images in sort order
     */
    @Named("imageResponses")
    default List<ProductDetailResponse.Image> imageResponses(List<ProductImage> images) {
        if (images == null) {
            return new ArrayList<>();
        }
        return images.stream()
                .sorted(Comparator.comparingInt(ProductImage::getSortOrder))
                .map(this::toImageResponse)
                .toList();
    }

    /**
     * Highlight texts in sort order
     */
    @Named("highlightTexts")
    default List<String> highlightTexts(List<ProductHighlight> highlights) {
        if (highlights == null) {
            return new ArrayList<>();
        }
        return highlights.stream()
                .sorted(Comparator.comparingInt(ProductHighlight::getSortOrder))
                .map(ProductHighlight::getText)
                .toList();
    }

    /**
     * Groups specification rows by group name: rows are taken in sort order, groups appear in
     * the order of their first row, and each group's items keep sort order.
     */
    @Named("groupSpecifications")
    default List<ProductDetailResponse.SpecificationGroup> groupSpecifications(List<ProductSpecification> specifications) {
        List<ProductDetailResponse.SpecificationGroup> groups = new ArrayList<>();
        if (specifications == null) {
            return groups;
        }
        Map<String, ProductDetailResponse.SpecificationGroup> byName = new LinkedHashMap<>();
        specifications.stream()
                .sorted(Comparator.comparingInt(ProductSpecification::getSortOrder))
                .forEach(spec -> byName
                        .computeIfAbsent(spec.getGroupName(), name -> ProductDetailResponse.SpecificationGroup.builder()
                                .group(name)
                                .items(new ArrayList<>())
                                .build())
                        .getItems()
                        .add(ProductDetailResponse.SpecificationItem.builder()
                                .key(spec.getSpecKey())
                                .value(spec.getSpecValue())
                                .build()));
        groups.addAll(byName.values());
        return groups;
    }
}
