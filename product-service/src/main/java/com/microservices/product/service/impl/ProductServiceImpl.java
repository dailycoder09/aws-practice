package com.microservices.product.service.impl;

import com.microservices.product.client.InventoryClient;
import com.microservices.product.dto.request.ProductRequest;
import com.microservices.product.dto.response.ProductResponse;
import com.microservices.product.exception.DuplicateSkuException;
import com.microservices.product.exception.ProductNotFoundException;
import com.microservices.product.mapper.ProductMapper;
import com.microservices.product.model.Product;
import com.microservices.product.repository.ProductRepository;
import com.microservices.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product Service Implementation
 * 
 * Implements business logic for Product management
 * Uses @Transactional for database transaction management
 * Comprehensive logging for monitoring and debugging
 * 
 * @author Microservices Team
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final InventoryClient inventoryClient;

    /**
     * Create a new product
     * Validates SKU uniqueness before creation
     */
    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creating new product with SKU: {}", request.getSkuCode());
        
        // Validate SKU uniqueness
        if (productRepository.existsBySkuCode(request.getSkuCode())) {
            log.warn("Attempt to create product with duplicate SKU: {}", request.getSkuCode());
            throw new DuplicateSkuException(request.getSkuCode());
        }

        // Map request to entity and save
        Product product = productMapper.toEntity(request);
        Product savedProduct = productRepository.save(product);
        
        log.info("Successfully created product with ID: {} and SKU: {}", 
                 savedProduct.getId(), savedProduct.getSkuCode());
        
        return productMapper.toResponse(savedProduct);
    }

    /**
     * Get product by ID
     */
    @Override
    public ProductResponse getProductById(Long id) {
        log.debug("Fetching product with ID: {}", id);
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Product not found with ID: {}", id);
                    return new ProductNotFoundException(id);
                });

        ProductResponse response = productMapper.toResponse(product);
        response.setStockQuantity(inventoryClient.findStockBySkuCode(response.getSkuCode()).orElse(null));

        log.debug("Successfully retrieved product with ID: {}", id);
        return response;
    }

    /**
     * Get product by SKU code
     */
    @Override
    public ProductResponse getProductBySkuCode(String skuCode) {
        log.debug("Fetching product with SKU: {}", skuCode);
        
        Product product = productRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> {
                    log.error("Product not found with SKU: {}", skuCode);
                    return new ProductNotFoundException(skuCode);
                });

        ProductResponse response = productMapper.toResponse(product);
        response.setStockQuantity(inventoryClient.findStockBySkuCode(response.getSkuCode()).orElse(null));

        log.debug("Successfully retrieved product with SKU: {}", skuCode);
        return response;
    }

    /**
     * Get all products with pagination
     */
    @Override
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        log.debug("Fetching all products with pagination - Page: {}, Size: {}", 
                  pageable.getPageNumber(), pageable.getPageSize());
        
        Page<Product> productsPage = productRepository.findAll(pageable);
        
        log.debug("Retrieved {} products out of {} total", 
                  productsPage.getNumberOfElements(), productsPage.getTotalElements());
        
        return productsPage.map(productMapper::toResponse);
    }

    /**
     * Get all products without pagination
     */
    @Override
    public List<ProductResponse> getAllProducts() {
        log.debug("Fetching all products without pagination");
        
        List<Product> products = productRepository.findAll();
        
        log.debug("Retrieved {} products", products.size());
        
        return productMapper.toResponseList(products);
    }

    /**
     * Update existing product
     * Validates SKU uniqueness if SKU is being changed
     */
    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        log.info("Updating product with ID: {}", id);
        
        // Find existing product
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Cannot update - Product not found with ID: {}", id);
                    return new ProductNotFoundException(id);
                });

        // Check if SKU is being changed and if new SKU already exists
        if (!existingProduct.getSkuCode().equals(request.getSkuCode())) {
            log.debug("SKU code is being updated from {} to {}", 
                      existingProduct.getSkuCode(), request.getSkuCode());
            
            if (productRepository.existsBySkuCode(request.getSkuCode())) {
                log.warn("Cannot update - SKU {} already exists", request.getSkuCode());
                throw new DuplicateSkuException(request.getSkuCode());
            }
        }

        // Update entity and save
        productMapper.updateEntityFromRequest(request, existingProduct);
        Product updatedProduct = productRepository.save(existingProduct);
        
        log.info("Successfully updated product with ID: {}", id);
        
        return productMapper.toResponse(updatedProduct);
    }

    /**
     * Delete product by ID
     */
    @Override
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Deleting product with ID: {}", id);
        
        // Verify product exists before deletion
        if (!productRepository.existsById(id)) {
            log.error("Cannot delete - Product not found with ID: {}", id);
            throw new ProductNotFoundException(id);
        }

        productRepository.deleteById(id);
        
        log.info("Successfully deleted product with ID: {}", id);
    }

    /**
     * Get products by category with pagination
     */
    @Override
    public Page<ProductResponse> getProductsByCategory(String category, Pageable pageable) {
        log.debug("Fetching products by category: {} - Page: {}, Size: {}", 
                  category, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<Product> productsPage = productRepository.findByCategory(category, pageable);
        
        log.debug("Retrieved {} products in category '{}' out of {} total", 
                  productsPage.getNumberOfElements(), category, productsPage.getTotalElements());
        
        return productsPage.map(productMapper::toResponse);
    }

    /**
     * Get products by price range
     */
    @Override
    public Page<ProductResponse> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, 
                                                          Pageable pageable) {
        log.debug("Fetching products with price range: {} - {} - Page: {}, Size: {}", 
                  minPrice, maxPrice, pageable.getPageNumber(), pageable.getPageSize());
        
        // Validate price range
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            log.error("Invalid price range: min ({}) > max ({})", minPrice, maxPrice);
            throw new IllegalArgumentException("Minimum price cannot be greater than maximum price");
        }
        
        Page<Product> productsPage = productRepository.findByPriceBetween(minPrice, maxPrice, pageable);
        
        log.debug("Retrieved {} products in price range {} - {}", 
                  productsPage.getNumberOfElements(), minPrice, maxPrice);
        
        return productsPage.map(productMapper::toResponse);
    }

    /**
     * Search products by name
     */
    @Override
    public Page<ProductResponse> searchProductsByName(String name, Pageable pageable) {
        log.debug("Searching products by name: '{}' - Page: {}, Size: {}", 
                  name, pageable.getPageNumber(), pageable.getPageSize());
        
        Page<Product> productsPage = productRepository.findByNameContainingIgnoreCase(name, pageable);
        
        log.debug("Found {} products matching name '{}'", 
                  productsPage.getNumberOfElements(), name);
        
        return productsPage.map(productMapper::toResponse);
    }

    /**
     * Search products with multiple filters
     */
    @Override
    public Page<ProductResponse> searchProducts(String searchTerm, String category, 
                                                 BigDecimal minPrice, BigDecimal maxPrice, 
                                                 Pageable pageable) {
        log.debug("Searching products - Term: '{}', Category: '{}', Price: {} - {}, Page: {}, Size: {}", 
                  searchTerm, category, minPrice, maxPrice, 
                  pageable.getPageNumber(), pageable.getPageSize());
        
        // Validate price range if both are provided
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            log.error("Invalid price range in search: min ({}) > max ({})", minPrice, maxPrice);
            throw new IllegalArgumentException("Minimum price cannot be greater than maximum price");
        }
        
        Page<Product> productsPage = productRepository.searchProducts(
                searchTerm, category, minPrice, maxPrice, pageable);
        
        log.debug("Search returned {} products out of {} total matching criteria", 
                  productsPage.getNumberOfElements(), productsPage.getTotalElements());
        
        return productsPage.map(productMapper::toResponse);
    }

    /**
     * Get all distinct product categories
     */
    @Override
    public List<String> getAllCategories() {
        log.debug("Fetching all distinct product categories");
        
        List<String> categories = productRepository.findAllDistinctCategories();
        
        log.debug("Retrieved {} distinct categories", categories.size());
        
        return categories;
    }

    /**
     * Count products by category
     */
    @Override
    public long countProductsByCategory(String category) {
        log.debug("Counting products in category: {}", category);
        
        long count = productRepository.countByCategory(category);
        
        log.debug("Found {} products in category '{}'", count, category);
        
        return count;
    }

    /**
     * Check if product exists by SKU code
     */
    @Override
    public boolean existsBySkuCode(String skuCode) {
        log.debug("Checking if product exists with SKU: {}", skuCode);
        
        boolean exists = productRepository.existsBySkuCode(skuCode);
        
        log.debug("Product with SKU '{}' exists: {}", skuCode, exists);
        
        return exists;
    }
}
