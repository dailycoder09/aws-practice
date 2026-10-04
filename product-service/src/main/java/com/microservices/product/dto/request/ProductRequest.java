package com.microservices.product.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Product Request DTO
 * 
 * Used for creating new products
 * Includes Bean Validation annotations for input validation
 * 
 * @author Microservices Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    /**
     * Product name - required, 3-255 characters
     */
    @NotBlank(message = "Product name is required")
    @Size(min = 3, max = 255, message = "Product name must be between 3 and 255 characters")
    private String name;

    /**
     * Product description - optional, max 2000 characters
     */
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    /**
     * Product price - required, must be positive
     */
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @DecimalMax(value = "999999.99", message = "Price must not exceed 999,999.99")
    @Digits(integer = 6, fraction = 2, message = "Price must have at most 6 integer digits and 2 decimal places")
    private BigDecimal price;

    /**
     * Product category - optional, max 100 characters
     */
    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    /**
     * SKU Code - required, unique identifier
     */
    @NotBlank(message = "SKU code is required")
    @Pattern(regexp = "^[A-Z0-9-]{3,50}$", 
             message = "SKU code must be 3-50 characters, uppercase letters, numbers, and hyphens only")
    private String skuCode;
}
