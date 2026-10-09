package com.microservices.product.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product Request DTO
 *
 * Used for creating and updating products
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

    /**
     * Brand - optional, max 100 characters. Null on update leaves the stored value unchanged.
     */
    @Size(max = 100, message = "Brand must not exceed 100 characters")
    private String brand;

    /**
     * Maximum retail price - optional. Must not be lower than the price (checked in the service).
     * Null on update leaves the stored value unchanged.
     */
    @DecimalMin(value = "0.01", message = "MRP must be greater than 0")
    @DecimalMax(value = "999999.99", message = "MRP must not exceed 999,999.99")
    @Digits(integer = 6, fraction = 2, message = "MRP must have at most 6 integer digits and 2 decimal places")
    private BigDecimal mrp;

    /**
     * Warranty - optional, max 100 characters. Null on update leaves the stored value unchanged.
     */
    @Size(max = 100, message = "Warranty must not exceed 100 characters")
    private String warranty;

    /**
     * Seller - optional, max 100 characters. Null on update leaves the stored value unchanged.
     */
    @Size(max = 100, message = "Seller must not exceed 100 characters")
    private String seller;

    /**
     * Highlights - optional, up to 10 bullet points. Null on update leaves the stored list unchanged;
     * a provided list replaces it; an empty list clears it.
     */
    @Size(max = 10, message = "A product can have at most 10 highlights")
    private List<@NotBlank(message = "Highlight must not be blank")
                 @Size(max = 200, message = "Highlight must not exceed 200 characters") String> highlights;

    /**
     * Specifications - optional, up to 50 rows. Null on update leaves the stored list unchanged;
     * a provided list replaces it; an empty list clears it.
     */
    @Valid
    @Size(max = 50, message = "A product can have at most 50 specifications")
    private List<Specification> specifications;

    /**
     * One specification row; rows sharing the same group are shown together
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Specification {

        @NotBlank(message = "Specification group is required")
        @Size(max = 100, message = "Specification group must not exceed 100 characters")
        private String group;

        @NotBlank(message = "Specification key is required")
        @Size(max = 100, message = "Specification key must not exceed 100 characters")
        private String key;

        @NotBlank(message = "Specification value is required")
        @Size(max = 255, message = "Specification value must not exceed 255 characters")
        private String value;
    }
}
