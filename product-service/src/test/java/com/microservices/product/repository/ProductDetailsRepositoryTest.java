package com.microservices.product.repository;

import com.microservices.product.model.Product;
import com.microservices.product.model.ProductHighlight;
import com.microservices.product.model.ProductImage;
import com.microservices.product.model.ProductSpecification;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs Flyway V1-V4 against embedded H2 and checks the seeded product-detail data,
 * the JPA mappings (ddl-auto is validate) and the delete cascade.
 */
@DataJpaTest
class ProductDetailsRepositoryTest {

    private static final int ALL_PRODUCTS = 5010;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    private long count(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    // ------------------------------------------------------------------ seed completeness (all 5,010)

    @Test
    void seed_coversAllFiveThousandAndTenProducts() {
        assertThat(count("SELECT COUNT(*) FROM products")).isEqualTo(ALL_PRODUCTS);
    }

    @Test
    void seed_everyProductHasABrand() {
        assertThat(count("SELECT COUNT(*) FROM products WHERE brand IS NULL OR TRIM(brand) = ''")).isZero();
    }

    @Test
    void seed_everyProductHasAnMrpAtOrAbovePrice() {
        assertThat(count("SELECT COUNT(*) FROM products WHERE mrp IS NULL OR mrp < price")).isZero();
    }

    @Test
    void seed_generatedMrpIsWithinOnePointOneAndOnePointSixTimesPrice() {
        // 10 hand-written rows excluded: only the 5,000 generated SKUs follow the 1.10 - 1.60 rule
        assertThat(count("SELECT COUNT(*) FROM products WHERE LENGTH(sku_code) = 10 "
                + "AND REGEXP_LIKE(sku_code, '^[A-Z]{4}-[0-9]{5}$') "
                + "AND (mrp < ROUND(price * 1.10, 2) OR mrp > ROUND(price * 1.60, 2) + 0.01)")).isZero();
    }

    @Test
    void seed_everyProductHasWarrantyAndSeller() {
        assertThat(count("SELECT COUNT(*) FROM products WHERE warranty IS NULL OR seller IS NULL")).isZero();
    }

    @Test
    void seed_everyProductHasAnImageUrlEqualToItsFirstImage() {
        assertThat(count("SELECT COUNT(*) FROM products WHERE image_url IS NULL")).isZero();
        assertThat(count("SELECT COUNT(*) FROM products p WHERE p.image_url <> "
                + "(SELECT i.url FROM product_images i WHERE i.product_id = p.id AND i.sort_order = 1)")).isZero();
    }

    @Test
    void seed_everyProductHasExactlyFourImages() {
        assertThat(count("SELECT COUNT(*) FROM product_images")).isEqualTo(ALL_PRODUCTS * 4L);
        assertThat(count("SELECT COUNT(DISTINCT product_id) FROM product_images")).isEqualTo(ALL_PRODUCTS);
        assertThat(count("SELECT COUNT(*) FROM (SELECT product_id FROM product_images "
                + "GROUP BY product_id HAVING COUNT(*) <> 4)")).isZero();
        assertThat(count("SELECT COUNT(*) FROM (SELECT product_id FROM product_images "
                + "GROUP BY product_id HAVING MIN(sort_order) <> 1 OR MAX(sort_order) <> 4)")).isZero();
    }

    @Test
    void seed_imagesAreDummyPlaceholderUrlsForTheProductSku() {
        assertThat(count("SELECT COUNT(*) FROM product_images i JOIN products p ON p.id = i.product_id "
                + "WHERE i.url <> 'https://placehold.co/800x800/' "
                + "|| CASE i.sort_order WHEN 1 THEN '172740' WHEN 2 THEN '1D3050' WHEN 3 THEN '2A3F5F' ELSE '0F1B2D' END "
                + "|| '/E6EDF7/png?text=' || p.sku_code || '+' || CAST(i.sort_order AS VARCHAR)")).isZero();
        assertThat(count("SELECT COUNT(*) FROM product_images i JOIN products p ON p.id = i.product_id "
                + "WHERE i.alt <> p.name || ' - view ' || CAST(i.sort_order AS VARCHAR)")).isZero();
    }

    @Test
    void seed_everyProductHasAtLeastThreeHighlights() {
        assertThat(count("SELECT COUNT(*) FROM products p WHERE "
                + "(SELECT COUNT(*) FROM product_highlights h WHERE h.product_id = p.id) < 3")).isZero();
    }

    @Test
    void seed_everyProductHasAtLeastSixSpecificationRows() {
        assertThat(count("SELECT COUNT(*) FROM products p WHERE "
                + "(SELECT COUNT(*) FROM product_specifications s WHERE s.product_id = p.id) < 6")).isZero();
    }

    @Test
    void seed_noTemplatePlaceholderLeaksIntoTheData() {
        assertThat(count("SELECT COUNT(*) FROM product_highlights WHERE text LIKE '%{%' OR text LIKE '%}%'")).isZero();
        assertThat(count("SELECT COUNT(*) FROM product_specifications "
                + "WHERE spec_value LIKE '%{%' OR spec_value LIKE '%}%'")).isZero();
    }

    @Test
    void seed_generatedProductsUseTwoSpecificationGroupsEach() {
        assertThat(count("SELECT COUNT(*) FROM products p WHERE REGEXP_LIKE(p.sku_code, '^[A-Z]{4}-[0-9]{5}$') "
                + "AND (SELECT COUNT(DISTINCT s.group_name) FROM product_specifications s WHERE s.product_id = p.id) <> 2"))
                .isZero();
    }

    @Test
    void seed_isDeterministic_sameSkuAlwaysGetsTheSameBrandPoolPerCategory() {
        // 6 made-up brands per category, picked by MOD(id, 6)
        assertThat(count("SELECT COUNT(DISTINCT brand) FROM products WHERE sku_code LIKE 'ELEC-%'")).isEqualTo(6);
        assertThat(count("SELECT COUNT(DISTINCT brand) FROM products WHERE sku_code LIKE 'BOOK-%'")).isEqualTo(6);
    }

    // ------------------------------------------------------------------ the 10 hand-written products

    @ParameterizedTest
    @CsvSource({
            "APPLE-IP15P-128, Apple",
            "APPLE-MBP16-M3, Apple",
            "APPLE-APP2-WHT, Apple",
            "SAMSUNG-S24-256, Samsung",
            "SONY-WH1000XM5, Sony",
            "DELL-XPS15-I9, Dell",
            "APPLE-IPAD-AIR, Apple",
            "AMAZON-KDL-PW, Amazon",
            "GOPRO-H12-BLK, GoPro",
            "DYSON-V15-DETECT, Dyson"
    })
    void handWrittenProducts_haveTheirOwnBrandAndRichDetails(String sku, String brand) {
        Product product = productRepository.findBySkuCode(sku).orElseThrow();

        assertThat(product.getBrand()).isEqualTo(brand);
        assertThat(product.getMrp()).isGreaterThan(product.getPrice());
        assertThat(product.getWarranty()).isNotBlank();
        assertThat(product.getSeller()).isNotBlank();
        assertThat(product.getImageUrl()).contains("text=" + sku + "+1");

        assertThat(product.getImages()).hasSize(4);
        assertThat(product.getHighlights()).hasSizeBetween(4, 5);
        assertThat(product.getSpecifications()).hasSizeBetween(6, 10);
        assertThat(product.getSpecifications().stream().map(ProductSpecification::getGroupName).distinct().count())
                .isBetween(2L, 3L);
    }

    @Test
    void handWrittenProduct_keepsItsQuoteInTheImageAltText() {
        Product macbook = productRepository.findBySkuCode("APPLE-MBP16-M3").orElseThrow();

        assertThat(macbook.getImages()).extracting(ProductImage::getAlt)
                .containsExactly("MacBook Pro 16\" - view 1", "MacBook Pro 16\" - view 2",
                        "MacBook Pro 16\" - view 3", "MacBook Pro 16\" - view 4");
    }

    // ------------------------------------------------------------------ entity mapping

    @Test
    void childCollections_loadInSortOrder() {
        Product product = productRepository.findBySkuCode("APPLE-IP15P-128").orElseThrow();

        assertThat(product.getImages()).extracting(ProductImage::getSortOrder).containsExactly(1, 2, 3, 4);
        assertThat(product.getHighlights()).extracting(ProductHighlight::getSortOrder).containsExactly(1, 2, 3, 4, 5);
        assertThat(product.getSpecifications()).extracting(ProductSpecification::getSortOrder)
                .isSorted();
    }

    @Test
    void toString_doesNotInitialiseLazyCollections() {
        Product product = productRepository.findBySkuCode("APPLE-IP15P-128").orElseThrow();
        entityManager.clear();
        Product fresh = productRepository.findBySkuCode("APPLE-IP15P-128").orElseThrow();

        String text = fresh.toString();

        assertThat(text).contains("APPLE-IP15P-128");
        assertThat(Hibernate.isInitialized(fresh.getImages())).isFalse();
        assertThat(Hibernate.isInitialized(fresh.getHighlights())).isFalse();
        assertThat(Hibernate.isInitialized(fresh.getSpecifications())).isFalse();
        assertThat(product.getId()).isEqualTo(fresh.getId());
    }

    @Test
    void childToString_doesNotRecurseIntoTheOwningProduct() {
        Product product = productRepository.findBySkuCode("APPLE-IP15P-128").orElseThrow();

        String text = product.getImages().get(0).toString();

        assertThat(text).contains("placehold.co").doesNotContain("Product(");
    }

    @Test
    void savingAProductWithChildren_cascadesToAllThreeTables() {
        Product product = Product.builder()
                .name("Cascade Test Product")
                .price(new BigDecimal("10.00"))
                .mrp(new BigDecimal("12.50"))
                .skuCode("CASCADE-TEST-1")
                .brand("TestBrand")
                .build();
        product.getImages().add(ProductImage.builder().product(product).url("https://x/1.png").sortOrder(1).build());
        product.getHighlights().add(ProductHighlight.builder().product(product).text("h").sortOrder(1).build());
        product.getSpecifications().add(ProductSpecification.builder().product(product)
                .groupName("G").specKey("k").specValue("v").sortOrder(1).build());

        Product saved = productRepository.saveAndFlush(product);

        assertThat(count("SELECT COUNT(*) FROM product_images WHERE product_id = ?", saved.getId())).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM product_highlights WHERE product_id = ?", saved.getId())).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM product_specifications WHERE product_id = ?", saved.getId())).isEqualTo(1);
    }

    // ------------------------------------------------------------------ deleting a product

    @Test
    void deletingAProduct_removesItsImagesHighlightsAndSpecifications() {
        Product product = productRepository.findBySkuCode("APPLE-IP15P-128").orElseThrow();
        Long id = product.getId();
        assertThat(count("SELECT COUNT(*) FROM product_images WHERE product_id = ?", id)).isEqualTo(4);
        long otherImages = count("SELECT COUNT(*) FROM product_images WHERE product_id <> ?", id);

        productRepository.deleteById(id);
        productRepository.flush();
        entityManager.clear();

        assertThat(productRepository.existsById(id)).isFalse();
        assertThat(count("SELECT COUNT(*) FROM product_images WHERE product_id = ?", id)).isZero();
        assertThat(count("SELECT COUNT(*) FROM product_highlights WHERE product_id = ?", id)).isZero();
        assertThat(count("SELECT COUNT(*) FROM product_specifications WHERE product_id = ?", id)).isZero();
        // other products are untouched
        assertThat(count("SELECT COUNT(*) FROM product_images WHERE product_id <> ?", id)).isEqualTo(otherImages);
    }

    @Test
    void deletingAProductWithPlainSql_isCascadedByTheForeignKeys() {
        Long id = productRepository.findBySkuCode("SONY-WH1000XM5").orElseThrow().getId();
        entityManager.clear();

        int deleted = jdbc.update("DELETE FROM products WHERE id = ?", id);

        assertThat(deleted).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM product_images WHERE product_id = ?", id)).isZero();
        assertThat(count("SELECT COUNT(*) FROM product_highlights WHERE product_id = ?", id)).isZero();
        assertThat(count("SELECT COUNT(*) FROM product_specifications WHERE product_id = ?", id)).isZero();
    }

    // ------------------------------------------------------------------ schema constraints

    @Test
    void negativeMrp_isRejectedByTheCheckConstraint() {
        assertThatThrownBy(() -> jdbc.update("UPDATE products SET mrp = -1 WHERE sku_code = 'APPLE-IP15P-128'"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void nullMrp_isAllowed() {
        int updated = jdbc.update("UPDATE products SET mrp = NULL WHERE sku_code = 'APPLE-IP15P-128'");

        assertThat(updated).isEqualTo(1);
    }

    @Test
    void duplicateImagePositionForTheSameProduct_isRejectedByTheUniqueConstraint() {
        Long id = productRepository.findBySkuCode("APPLE-IP15P-128").orElseThrow().getId();

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO product_images (product_id, url, sort_order) VALUES (?, 'https://x/dup.png', 1)", id))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void childRowForAnUnknownProduct_isRejectedByTheForeignKey() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO product_highlights (product_id, text, sort_order) VALUES (-1, 'orphan', 1)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
