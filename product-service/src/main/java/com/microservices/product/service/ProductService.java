package com.microservices.product.service;

import com.microservices.product.dto.request.ProductRequest;
import com.microservices.product.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product Service Interface
 * 
 * Defines business operations for Product management
 * Follows Interface Segregation Principle for clean architecture
 * 
 * @author Microservices Team
 * @version 1.0
 */
public interface ProductService {

    /**
     * Create a new product
     * 
     * @param request Product creation request
     * @return Created product response
     * @throws com.microservices.product.exception.DuplicateSkuException if SKU already exists
     */
    ProductResponse createProduct(ProductRequest request);

    /**
     * Get product by ID
     * 
     * @param id Product ID
     * @return Product response
     * @throws com.microservices.product.exception.ProductNotFoundException if product not found
     */
    ProductResponse getProductById(Long id);

    /**
     * Get product by SKU code
     * 
     * @param skuCode Stock Keeping Unit code
     * @return Product response
     * @throws com.microservices.product.exception.ProductNotFoundException if product not found
     */
    ProductResponse getProductBySkuCode(String skuCode);

    /**
     * Get all products with pagination
     * 
     * @param pageable Pagination parameters
     * @return Page of product responses
     */
    Page<ProductResponse> getAllProducts(Pageable pageable);

    /**
     * Get all products without pagination
     * 
     * @return List of all product responses
     */
    List<ProductResponse> getAllProducts();

    /**
     * Update existing product
     * 
     * @param id Product ID
     * @param request Product update request
     * @return Updated product response
     * @throws com.microservices.product.exception.ProductNotFoundException if product not found
     * @throws com.microservices.product.exception.DuplicateSkuException if new SKU already exists
     */
    ProductResponse updateProduct(Long id, ProductRequest request);

    /**
     * Delete product by ID
     * 
     * @param id Product ID
     * @throws com.microservices.product.exception.ProductNotFoundException if product not found
     */
    void deleteProduct(Long id);

    /**
     * Get products by category with pagination
     * 
     * @param category Product category
     * @param pageable Pagination parameters
     * @return Page of product responses
     */
    Page<ProductResponse> getProductsByCategory(String category, Pageable pageable);

    /**
     * Get products by price range
     * 
     * @param minPrice Minimum price
     * @param maxPrice Maximum price
     * @param pageable Pagination parameters
     * @return Page of product responses
     */
    Page<ProductResponse> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /**
     * Search products by name
     * 
     * @param name Search term for product name
     * @param pageable Pagination parameters
     * @return Page of product responses
     */
    Page<ProductResponse> searchProductsByName(String name, Pageable pageable);

    /**
     * Search products with multiple filters
     * 
     * @param searchTerm Search term for name or description
     * @param category Product category filter
     * @param minPrice Minimum price filter
     * @param maxPrice Maximum price filter
     * @param pageable Pagination parameters
     * @return Page of product responses matching criteria
     */
    Page<ProductResponse> searchProducts(String searchTerm, String category, 
                                         BigDecimal minPrice, BigDecimal maxPrice, 
                                         Pageable pageable);

    /**
     * Get all distinct product categories
     * 
     * @return List of unique categories
     */
    List<String> getAllCategories();

    /**
     * Count products by category
     * 
     * @param category Product category
     * @return Number of products in category
     */
    long countProductsByCategory(String category);

    /**
     * Check if product exists by SKU code
     * 
     * @param skuCode Stock Keeping Unit code
     * @return true if product exists, false otherwise
     */
    boolean existsBySkuCode(String skuCode);
}
