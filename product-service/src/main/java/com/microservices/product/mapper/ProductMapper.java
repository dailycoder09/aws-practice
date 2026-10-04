package com.microservices.product.mapper;

import com.microservices.product.dto.request.ProductRequest;
import com.microservices.product.dto.response.ProductResponse;
import com.microservices.product.model.Product;
import org.mapstruct.*;

import java.util.List;

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
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ProductMapper {

    /**
     * Maps ProductRequest DTO to Product entity
     * 
     * @param request Product request DTO
     * @return Product entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product toEntity(ProductRequest request);

    /**
     * Maps Product entity to ProductResponse DTO
     * 
     * @param product Product entity
     * @return Product response DTO
     */
    ProductResponse toResponse(Product product);

    /**
     * Maps list of Product entities to list of ProductResponse DTOs
     * 
     * @param products List of product entities
     * @return List of product response DTOs
     */
    List<ProductResponse> toResponseList(List<Product> products);

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
    void updateEntityFromRequest(ProductRequest request, @MappingTarget Product product);
}
