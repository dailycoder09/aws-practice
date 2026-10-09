package com.microservices.product.service.impl;

import com.microservices.product.client.InventoryClient;
import com.microservices.product.dto.request.ProductImagesRequest;
import com.microservices.product.dto.request.ProductRequest;
import com.microservices.product.dto.response.ProductDetailResponse;
import com.microservices.product.dto.response.ProductResponse;
import com.microservices.product.exception.DuplicateSkuException;
import com.microservices.product.exception.ProductNotFoundException;
import com.microservices.product.mapper.ProductMapper;
import com.microservices.product.model.Product;
import com.microservices.product.model.ProductHighlight;
import com.microservices.product.model.ProductImage;
import com.microservices.product.model.ProductSpecification;
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

        validateMrp(request);

        // Validate SKU uniqueness
        if (productRepository.existsBySkuCode(request.getSkuCode())) {
            log.warn("Attempt to create product with duplicate SKU: {}", request.getSkuCode());
            throw new DuplicateSkuException(request.getSkuCode());
        }

        // Map request to entity (scalar fields), handle the child collections explicitly, and save
        Product product = productMapper.toEntity(request);
        if (request.getHighlights() != null) {
            replaceHighlights(product, request.getHighlights());
        }
        if (request.getSpecifications() != null) {
            replaceSpecifications(product, request.getSpecifications());
        }
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
     * Get full product details by ID
     */
    @Override
    public ProductDetailResponse getProductDetailsById(Long id) {
        log.debug("Fetching product details with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Product not found with ID: {}", id);
                    return new ProductNotFoundException(id);
                });

        return toDetailResponse(product);
    }

    /**
     * Get full product details by SKU code
     */
    @Override
    public ProductDetailResponse getProductDetailsBySkuCode(String skuCode) {
        log.debug("Fetching product details with SKU: {}", skuCode);

        Product product = productRepository.findBySkuCode(skuCode)
                .orElseThrow(() -> {
                    log.error("Product not found with SKU: {}", skuCode);
                    return new ProductNotFoundException(skuCode);
                });

        return toDetailResponse(product);
    }

    /**
     * Replace the whole image list of a product and update the denormalised primary image URL
     */
    @Override
    @Transactional
    public ProductDetailResponse replaceProductImages(Long id, ProductImagesRequest request) {
        log.info("Replacing images of product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Cannot replace images - Product not found with ID: {}", id);
                    return new ProductNotFoundException(id);
                });

        List<ProductImagesRequest.Image> newImages = request.getImages();
        if (newImages == null) {
            throw new IllegalArgumentException("Images are required (use an empty list to remove all images)");
        }

        // Delete the old rows first: images are unique per (product_id, sort_order) and Hibernate
        // flushes inserts before orphan deletes, so re-inserting position 1..n in the same flush
        // would violate the unique constraint.
        product.getImages().clear();
        productRepository.flush();

        int position = 1;
        for (ProductImagesRequest.Image image : newImages) {
            product.getImages().add(ProductImage.builder()
                    .product(product)
                    .url(image.getUrl())
                    .alt(image.getAlt())
                    .sortOrder(position++)
                    .build());
        }
        product.setImageUrl(newImages.isEmpty() ? null : newImages.get(0).getUrl());

        Product savedProduct = productRepository.save(product);

        log.info("Product {} now has {} image(s)", id, newImages.size());
        return toDetailResponse(savedProduct);
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

        validateMrp(request);

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

        // Update scalar fields (null = unchanged), then the child collections explicitly:
        // null = leave unchanged, a provided list replaces the stored one, an empty list clears it
        productMapper.updateEntityFromRequest(request, existingProduct);
        if (request.getHighlights() != null) {
            replaceHighlights(existingProduct, request.getHighlights());
        }
        if (request.getSpecifications() != null) {
            replaceSpecifications(existingProduct, request.getSpecifications());
        }
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

    /**
     * Cross-field rule: when both mrp and price are supplied, mrp must not be lower than the price
     */
    private void validateMrp(ProductRequest request) {
        if (request.getMrp() != null && request.getPrice() != null
                && request.getMrp().compareTo(request.getPrice()) < 0) {
            log.warn("Rejected request for SKU {}: MRP {} is lower than price {}",
                     request.getSkuCode(), request.getMrp(), request.getPrice());
            throw new IllegalArgumentException("MRP cannot be lower than the price");
        }
    }

    /**
     * Replace the highlight rows of a product in place (the list instance must be kept for orphanRemoval)
     */
    private void replaceHighlights(Product product, List<String> texts) {
        product.getHighlights().clear();
        int position = 1;
        for (String text : texts) {
            product.getHighlights().add(ProductHighlight.builder()
                    .product(product)
                    .text(text)
                    .sortOrder(position++)
                    .build());
        }
    }

    /**
     * Replace the specification rows of a product in place (the list instance must be kept for orphanRemoval)
     */
    private void replaceSpecifications(Product product, List<ProductRequest.Specification> specifications) {
        product.getSpecifications().clear();
        int position = 1;
        for (ProductRequest.Specification specification : specifications) {
            product.getSpecifications().add(ProductSpecification.builder()
                    .product(product)
                    .groupName(specification.getGroup())
                    .specKey(specification.getKey())
                    .specValue(specification.getValue())
                    .sortOrder(position++)
                    .build());
        }
    }

    /**
     * Map a product to its detail response and fill stockQuantity from inventory-service
     * (fail-open: null when inventory-service has no record or is unavailable)
     */
    private ProductDetailResponse toDetailResponse(Product product) {
        ProductDetailResponse response = productMapper.toDetailResponse(product);
        response.setStockQuantity(inventoryClient.findStockBySkuCode(product.getSkuCode()).orElse(null));
        return response;
    }
}
