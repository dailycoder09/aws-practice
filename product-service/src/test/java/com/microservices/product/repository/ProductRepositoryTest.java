package com.microservices.product.repository;

import com.microservices.product.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryTest {

    @org.springframework.beans.factory.annotation.Autowired
    private ProductRepository productRepository;

    @Test
    void searchProducts_matchesByNameAndCategoryAndPriceRange() {
        Pageable pageable = PageRequest.of(0, 20);

        var results = productRepository.searchProducts(
                "iPhone", "Electronics",
                new BigDecimal("500"), new BigDecimal("1500"),
                pageable);

        assertThat(results.getContent())
                .extracting(Product::getSkuCode)
                .contains("APPLE-IP15P-128");
    }

    @Test
    void searchProducts_excludesOutOfRangeResults() {
        Pageable pageable = PageRequest.of(0, 20);

        var results = productRepository.searchProducts(
                "iPhone", "Electronics",
                new BigDecimal("0"), new BigDecimal("100"),
                pageable);

        assertThat(results.getContent()).isEmpty();
    }

    @Test
    void findByCategoryAndPriceRange_filtersCorrectly() {
        Pageable pageable = PageRequest.of(0, 20);

        // Narrow to the exact seeded price to isolate the single V1 "Dyson" row
        // out of the ~500 bulk-seeded Home Appliances rows from V2.
        var results = productRepository.findByCategoryAndPriceRange(
                "Home Appliances", new BigDecimal("649.99"), new BigDecimal("649.99"), pageable);

        assertThat(results.getContent())
                .extracting(Product::getSkuCode)
                .containsExactly("DYSON-V15-DETECT");
    }

    @Test
    void findAllDistinctCategories_returnsSortedUniqueCategories() {
        List<String> categories = productRepository.findAllDistinctCategories();

        assertThat(categories).containsExactly(
                "Automotive", "Beauty and Personal Care", "Books", "Clothing", "Electronics",
                "Furniture", "Groceries", "Home Appliances", "Sports and Outdoors", "Toys and Games");
    }

    @Test
    void countByCategory_countsSeedProducts() {
        long count = productRepository.countByCategory("Electronics");

        // 9 from the V1 handwritten sample rows + 500 from the V2 bulk seed
        assertThat(count).isEqualTo(509);
    }

    @Test
    void existsBySkuCode_trueForSeededSku_falseForUnknown() {
        assertThat(productRepository.existsBySkuCode("APPLE-IP15P-128")).isTrue();
        assertThat(productRepository.existsBySkuCode("DOES-NOT-EXIST")).isFalse();
    }
}
