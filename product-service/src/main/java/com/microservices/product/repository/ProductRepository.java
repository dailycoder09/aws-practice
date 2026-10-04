package com.microservices.product.repository;

import com.microservices.product.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Product Repository
 * 
 * Provides data access operations for Product entity
 * Extends JpaRepository for CRUD operations and custom queries
 * 
 * @author Microservices Team
 * @version 1.0
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Find product by SKU code
     * 
     * @param skuCode Stock Keeping Unit code
     * @return Optional containing product if found
     */
    Optional<Product> findBySkuCode(String skuCode);

    /**
     * Check if product exists with given SKU code
     * 
     * @param skuCode Stock Keeping Unit code
     * @return true if product exists, false otherwise
     */
    boolean existsBySkuCode(String skuCode);

    /**
     * Find all products by category
     * 
     * @param category Product category
     * @param pageable Pagination information
     * @return Page of products in the specified category
     */
    Page<Product> findByCategory(String category, Pageable pageable);

    /**
     * Find all products by category (non-paginated)
     * 
     * @param category Product category
     * @return List of products in the specified category
     */
    List<Product> findByCategory(String category);

    /**
     * Find products with price between min and max
     * 
     * @param minPrice Minimum price
     * @param maxPrice Maximum price
     * @param pageable Pagination information
     * @return Page of products within price range
     */
    Page<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /**
     * Search products by name (case-insensitive, partial match)
     * 
     * @param name Product name to search
     * @param pageable Pagination information
     * @return Page of matching products
     */
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Find products by category and price range (custom query)
     * 
     * @param category Product category
     * @param minPrice Minimum price
     * @param maxPrice Maximum price
     * @param pageable Pagination information
     * @return Page of products matching criteria
     */
    @Query("SELECT p FROM Product p WHERE " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Product> findByCategoryAndPriceRange(
        @Param("category") String category,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        Pageable pageable
    );

    /**
     * Search products with multiple filters
     * 
     * @param searchTerm Search term for name or description
     * @param category Product category
     * @param minPrice Minimum price
     * @param maxPrice Maximum price
     * @param pageable Pagination information
     * @return Page of products matching search criteria
     */
    @Query("SELECT p FROM Product p WHERE " +
           "(:searchTerm IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "(:category IS NULL OR p.category = :category) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Product> searchProducts(
        @Param("searchTerm") String searchTerm,
        @Param("category") String category,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        Pageable pageable
    );

    /**
     * Get all distinct categories
     * 
     * @return List of unique product categories
     */
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.category IS NOT NULL ORDER BY p.category")
    List<String> findAllDistinctCategories();

    /**
     * Count products by category
     * 
     * @param category Product category
     * @return Number of products in the category
     */
    long countByCategory(String category);
}
