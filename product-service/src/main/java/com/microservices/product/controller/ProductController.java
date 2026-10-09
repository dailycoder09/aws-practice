package com.microservices.product.controller;

import com.microservices.product.dto.request.ProductImagesRequest;
import com.microservices.product.dto.request.ProductRequest;
import com.microservices.product.dto.response.ProductDetailResponse;
import com.microservices.product.dto.response.ProductResponse;
import com.microservices.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product REST Controller
 * 
 * Provides RESTful API endpoints for Product management
 * Follows REST best practices with proper HTTP methods and status codes
 * Comprehensive API documentation with Swagger/OpenAPI annotations
 * 
 * @author Microservices Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Product Management", description = "APIs for managing products in the catalog")
public class ProductController {

    private final ProductService productService;

    /**
     * Create a new product
     * 
     * @param request Product creation request
     * @return Created product response with HTTP 201
     */
    @PostMapping
    @Operation(summary = "Create a new product", description = "Creates a new product in the catalog")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Product created successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "409", description = "Product with SKU already exists")
    })
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request) {
        log.info("REST request to create product with SKU: {}", request.getSkuCode());
        
        ProductResponse response = productService.createProduct(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get product by ID
     * 
     * @param id Product ID
     * @return Product response with HTTP 200
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Retrieves a product by its unique identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product found",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductResponse> getProductById(
            @Parameter(description = "Product ID") @PathVariable Long id) {
        log.debug("REST request to get product with ID: {}", id);
        
        ProductResponse response = productService.getProductById(id);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get product by SKU code
     * 
     * @param skuCode Stock Keeping Unit code
     * @return Product response with HTTP 200
     */
    @GetMapping("/sku/{skuCode}")
    @Operation(summary = "Get product by SKU", description = "Retrieves a product by its SKU code")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product found",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductResponse> getProductBySkuCode(
            @Parameter(description = "SKU Code") @PathVariable String skuCode) {
        log.debug("REST request to get product with SKU: {}", skuCode);
        
        ProductResponse response = productService.getProductBySkuCode(skuCode);

        return ResponseEntity.ok(response);
    }

    /**
     * Get full product details by ID
     *
     * @param id Product ID
     * @return Product detail response with HTTP 200
     */
    // Digits only: a plain "/{id}/details" would also capture "/sku/details" (id = "sku"), which must
    // stay a normal /sku/{skuCode} lookup (404 for an unknown SKU) instead of failing with a type mismatch.
    @GetMapping("/{id:\\d+}/details")
    @Operation(summary = "Get product details by ID",
               description = "Retrieves the full detail page data (images, highlights, grouped specifications) of a product")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product found",
                    content = @Content(schema = @Schema(implementation = ProductDetailResponse.class))),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductDetailResponse> getProductDetailsById(
            @Parameter(description = "Product ID") @PathVariable Long id) {
        log.debug("REST request to get product details with ID: {}", id);

        ProductDetailResponse response = productService.getProductDetailsById(id);

        return ResponseEntity.ok(response);
    }

    /**
     * Get full product details by SKU code
     *
     * @param skuCode Stock Keeping Unit code
     * @return Product detail response with HTTP 200
     */
    @GetMapping("/sku/{skuCode}/details")
    @Operation(summary = "Get product details by SKU",
               description = "Retrieves the full detail page data (images, highlights, grouped specifications) of a product by its SKU code")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product found",
                    content = @Content(schema = @Schema(implementation = ProductDetailResponse.class))),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductDetailResponse> getProductDetailsBySkuCode(
            @Parameter(description = "SKU Code") @PathVariable String skuCode) {
        log.debug("REST request to get product details with SKU: {}", skuCode);

        ProductDetailResponse response = productService.getProductDetailsBySkuCode(skuCode);

        return ResponseEntity.ok(response);
    }

    /**
     * Replace the whole image list of a product
     *
     * @param id Product ID
     * @param request New image list (0-10 http/https URLs)
     * @return Updated product detail response with HTTP 200
     */
    @PutMapping("/{id}/images")
    @Operation(summary = "Replace product images",
               description = "Replaces the whole image list of a product with the given http/https URLs "
                       + "(0-10 items, an empty list removes all images) and updates the primary image URL")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Images replaced successfully",
                    content = @Content(schema = @Schema(implementation = ProductDetailResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductDetailResponse> replaceProductImages(
            @Parameter(description = "Product ID") @PathVariable Long id,
            @Valid @RequestBody ProductImagesRequest request) {
        log.info("REST request to replace images of product with ID: {}", id);

        ProductDetailResponse response = productService.replaceProductImages(id, request);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all products with pagination
     * 
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @param sortBy Sort field (default: id)
     * @param sortDir Sort direction (default: asc)
     * @return Page of products with HTTP 200
     */
    @GetMapping
    @Operation(summary = "Get all products", description = "Retrieves all products with pagination and sorting")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    })
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "asc") String sortDir) {
        log.debug("REST request to get all products - Page: {}, Size: {}, Sort: {} {}", 
                  page, size, sortBy, sortDir);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProductResponse> products = productService.getAllProducts(pageable);
        
        return ResponseEntity.ok(products);
    }

    /**
     * Update existing product
     * 
     * @param id Product ID
     * @param request Product update request
     * @return Updated product response with HTTP 200
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update product", description = "Updates an existing product")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product updated successfully",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "409", description = "Product with new SKU already exists")
    })
    public ResponseEntity<ProductResponse> updateProduct(
            @Parameter(description = "Product ID") @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        log.info("REST request to update product with ID: {}", id);
        
        ProductResponse response = productService.updateProduct(id, request);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Delete product by ID
     * 
     * @param id Product ID
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product", description = "Deletes a product by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Product ID") @PathVariable Long id) {
        log.info("REST request to delete product with ID: {}", id);
        
        productService.deleteProduct(id);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * Get products by category
     * 
     * @param category Product category
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @param sortBy Sort field (default: id)
     * @param sortDir Sort direction (default: asc)
     * @return Page of products in category with HTTP 200
     */
    @GetMapping("/category/{category}")
    @Operation(summary = "Get products by category", description = "Retrieves all products in a specific category")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    })
    public ResponseEntity<Page<ProductResponse>> getProductsByCategory(
            @Parameter(description = "Product category") @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        log.debug("REST request to get products by category: {}", category);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProductResponse> products = productService.getProductsByCategory(category, pageable);
        
        return ResponseEntity.ok(products);
    }

    /**
     * Search products with multiple filters
     * 
     * @param searchTerm Search term for name or description
     * @param category Product category filter
     * @param minPrice Minimum price filter
     * @param maxPrice Maximum price filter
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @param sortBy Sort field (default: id)
     * @param sortDir Sort direction (default: asc)
     * @return Page of products matching criteria with HTTP 200
     */
    @GetMapping("/search")
    @Operation(summary = "Search products", description = "Search products with multiple filters")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    public ResponseEntity<Page<ProductResponse>> searchProducts(
            @Parameter(description = "Search term") @RequestParam(required = false) String searchTerm,
            @Parameter(description = "Category filter") @RequestParam(required = false) String category,
            @Parameter(description = "Minimum price") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price") @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        log.debug("REST request to search products with filters");
        
        Sort sort = sortDir.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProductResponse> products = productService.searchProducts(
                searchTerm, category, minPrice, maxPrice, pageable);
        
        return ResponseEntity.ok(products);
    }

    /**
     * Get all distinct categories
     * 
     * @return List of categories with HTTP 200
     */
    @GetMapping("/categories")
    @Operation(summary = "Get all categories", description = "Retrieves all distinct product categories")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Categories retrieved successfully")
    })
    public ResponseEntity<List<String>> getAllCategories() {
        log.debug("REST request to get all categories");
        
        List<String> categories = productService.getAllCategories();
        
        return ResponseEntity.ok(categories);
    }

    /**
     * Count products by category
     * 
     * @param category Product category
     * @return Product count with HTTP 200
     */
    @GetMapping("/category/{category}/count")
    @Operation(summary = "Count products by category", description = "Returns the number of products in a category")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Count retrieved successfully")
    })
    public ResponseEntity<Long> countProductsByCategory(
            @Parameter(description = "Product category") @PathVariable String category) {
        log.debug("REST request to count products in category: {}", category);
        
        long count = productService.countProductsByCategory(category);
        
        return ResponseEntity.ok(count);
    }

    /**
     * Check if product exists by SKU
     * 
     * @param skuCode Stock Keeping Unit code
     * @return Boolean indicating existence with HTTP 200
     */
    @GetMapping("/sku/{skuCode}/exists")
    @Operation(summary = "Check SKU existence", description = "Checks if a product with given SKU exists")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Check completed successfully")
    })
    public ResponseEntity<Boolean> existsBySkuCode(
            @Parameter(description = "SKU Code") @PathVariable String skuCode) {
        log.debug("REST request to check if product exists with SKU: {}", skuCode);
        
        boolean exists = productService.existsBySkuCode(skuCode);
        
        return ResponseEntity.ok(exists);
    }
}
