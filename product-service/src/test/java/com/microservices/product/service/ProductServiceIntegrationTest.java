package com.microservices.product.service;

import com.microservices.product.client.InventoryClient;
import com.microservices.product.dto.request.ProductImagesRequest;
import com.microservices.product.dto.request.ProductRequest;
import com.microservices.product.dto.response.ProductDetailResponse;
import com.microservices.product.dto.response.ProductResponse;
import com.microservices.product.exception.ProductNotFoundException;
import com.microservices.product.mapper.ProductMapperImpl;
import com.microservices.product.model.Product;
import com.microservices.product.repository.ProductRepository;
import com.microservices.product.service.impl.ProductServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Real service + real mapper + real H2 (Flyway V1-V4): proves the behaviour that mocks cannot -
 * orphan removal vs the unique image position, cascade on delete, grouped specifications, and
 * that list endpoints never load the lazy collections (no N+1).
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import({ProductServiceImpl.class, ProductMapperImpl.class})
class ProductServiceIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private JdbcTemplate jdbc;

    @MockBean
    private InventoryClient inventoryClient;

    private long count(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private Long idOf(String sku) {
        return productRepository.findBySkuCode(sku).orElseThrow().getId();
    }

    private ProductImagesRequest images(String... urls) {
        List<ProductImagesRequest.Image> list = new ArrayList<>();
        for (int i = 0; i < urls.length; i++) {
            list.add(new ProductImagesRequest.Image(urls[i], "alt " + (i + 1)));
        }
        return new ProductImagesRequest(list);
    }

    // ------------------------------------------------------------------ details

    @Test
    void getProductDetailsBySkuCode_returnsHandWrittenDetailsWithGroupedSpecifications() {
        when(inventoryClient.findStockBySkuCode("APPLE-IP15P-128")).thenReturn(Optional.of(25));

        ProductDetailResponse detail = productService.getProductDetailsBySkuCode("APPLE-IP15P-128");

        assertThat(detail.getBrand()).isEqualTo("Apple");
        assertThat(detail.getMrp()).isEqualByComparingTo("1099.99");
        assertThat(detail.getDiscountPercent()).isEqualTo(9);
        assertThat(detail.getStockQuantity()).isEqualTo(25);
        assertThat(detail.getImages()).hasSize(4);
        assertThat(detail.getImages().get(0).getUrl())
                .isEqualTo("https://placehold.co/800x800/172740/E6EDF7/png?text=APPLE-IP15P-128+1");
        assertThat(detail.getImages().get(0).getAlt()).isEqualTo("iPhone 15 Pro - view 1");
        assertThat(detail.getHighlights()).hasSize(5);
        assertThat(detail.getSpecifications())
                .extracting(ProductDetailResponse.SpecificationGroup::getGroup)
                .containsExactly("Display", "Performance", "Camera");
        assertThat(detail.getSpecifications().get(0).getItems())
                .extracting(ProductDetailResponse.SpecificationItem::getKey)
                .containsExactly("Screen Size", "Resolution", "Refresh Rate");
    }

    @Test
    void getProductDetailsById_returnsGeneratedDetails_andFailsOpenWithoutInventory() {
        when(inventoryClient.findStockBySkuCode(anyString())).thenReturn(Optional.empty());
        Long id = idOf("ELEC-00001");

        ProductDetailResponse detail = productService.getProductDetailsById(id);

        assertThat(detail.getSkuCode()).isEqualTo("ELEC-00001");
        assertThat(detail.getStockQuantity()).isNull();
        assertThat(detail.getBrand()).isNotBlank();
        assertThat(detail.getMrp()).isGreaterThan(detail.getPrice());
        assertThat(detail.getDiscountPercent()).isBetween(8, 38);
        assertThat(detail.getImages()).hasSize(4);
        assertThat(detail.getHighlights()).hasSizeBetween(3, 5);
        assertThat(detail.getSpecifications())
                .extracting(ProductDetailResponse.SpecificationGroup::getGroup)
                .containsExactly("General", "Technical");
        assertThat(detail.getSpecifications().stream().mapToInt(group -> group.getItems().size()).sum())
                .isBetween(6, 8);
    }

    @Test
    void getProductDetailsById_throwsProductNotFoundException_forUnknownId() {
        assertThatThrownBy(() -> productService.getProductDetailsById(999_999L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    // ------------------------------------------------------------------ replace images

    @Test
    void replaceProductImages_replacesFourImagesWithFourDifferentOnes_withoutBreakingTheUniquePosition() {
        Long id = idOf("APPLE-IP15P-128");

        ProductDetailResponse result = productService.replaceProductImages(id, images(
                "https://cdn.example.com/1.png", "https://cdn.example.com/2.png",
                "https://cdn.example.com/3.png", "https://cdn.example.com/4.png"));
        flushAndClear();

        assertThat(result.getImages()).extracting(ProductDetailResponse.Image::getUrl).containsExactly(
                "https://cdn.example.com/1.png", "https://cdn.example.com/2.png",
                "https://cdn.example.com/3.png", "https://cdn.example.com/4.png");
        assertThat(count("SELECT COUNT(*) FROM product_images WHERE product_id = ?", id)).isEqualTo(4);
        assertThat(jdbc.queryForList("SELECT url FROM product_images WHERE product_id = ? ORDER BY sort_order",
                String.class, id)).containsExactly(
                "https://cdn.example.com/1.png", "https://cdn.example.com/2.png",
                "https://cdn.example.com/3.png", "https://cdn.example.com/4.png");
        assertThat(jdbc.queryForObject("SELECT image_url FROM products WHERE id = ?", String.class, id))
                .isEqualTo("https://cdn.example.com/1.png");
    }

    @Test
    void replaceProductImages_canShrinkAndGrowTheList() {
        Long id = idOf("APPLE-IP15P-128");

        productService.replaceProductImages(id, images("https://cdn.example.com/only.png"));
        flushAndClear();
        assertThat(count("SELECT COUNT(*) FROM product_images WHERE product_id = ?", id)).isEqualTo(1);

        String[] ten = new String[10];
        for (int i = 0; i < ten.length; i++) {
            ten[i] = "https://cdn.example.com/g" + i + ".png";
        }
        productService.replaceProductImages(id, images(ten));
        flushAndClear();

        assertThat(count("SELECT COUNT(*) FROM product_images WHERE product_id = ?", id)).isEqualTo(10);
        assertThat(jdbc.queryForObject("SELECT image_url FROM products WHERE id = ?", String.class, id))
                .isEqualTo("https://cdn.example.com/g0.png");
    }

    @Test
    void replaceProductImages_withEmptyList_removesAllImagesAndClearsTheImageUrl() {
        Long id = idOf("APPLE-IP15P-128");

        ProductDetailResponse result = productService.replaceProductImages(id, images());
        flushAndClear();

        assertThat(result.getImages()).isEmpty();
        assertThat(count("SELECT COUNT(*) FROM product_images WHERE product_id = ?", id)).isZero();
        assertThat(jdbc.queryForObject("SELECT image_url FROM products WHERE id = ?", String.class, id)).isNull();
        // the light list view reflects it too
        assertThat(productService.getProductById(id).getImageUrl()).isNull();
    }

    @Test
    void replaceProductImages_throwsProductNotFoundException_forUnknownId() {
        assertThatThrownBy(() -> productService.replaceProductImages(999_999L, images("https://cdn.example.com/a.png")))
                .isInstanceOf(ProductNotFoundException.class);
    }

    // ------------------------------------------------------------------ create / update

    private ProductRequest.ProductRequestBuilder newProduct() {
        return ProductRequest.builder()
                .name("Integration Test Phone")
                .price(new BigDecimal("500.00"))
                .category("Electronics")
                .skuCode("IT-PHONE-1");
    }

    @Test
    void createProduct_withDetailFields_persistsThemAndStartsWithoutImages() {
        ProductRequest request = newProduct()
                .brand("TestBrand")
                .mrp(new BigDecimal("625.00"))
                .warranty("2 years")
                .seller("Test Seller")
                .highlights(List.of("first", "second", "third"))
                .specifications(List.of(
                        new ProductRequest.Specification("Display", "Size", "6 in"),
                        new ProductRequest.Specification("Battery", "Capacity", "4000 mAh"),
                        new ProductRequest.Specification("Display", "Type", "OLED")))
                .build();

        ProductResponse created = productService.createProduct(request);
        flushAndClear();

        assertThat(created.getBrand()).isEqualTo("TestBrand");
        assertThat(created.getMrp()).isEqualByComparingTo("625.00");
        assertThat(created.getDiscountPercent()).isEqualTo(20);
        assertThat(created.getImageUrl()).isNull();
        assertThat(jdbc.queryForList("SELECT text FROM product_highlights WHERE product_id = ? ORDER BY sort_order",
                String.class, created.getId())).containsExactly("first", "second", "third");
        assertThat(count("SELECT COUNT(*) FROM product_specifications WHERE product_id = ?", created.getId()))
                .isEqualTo(3);

        ProductDetailResponse detail = productService.getProductDetailsById(created.getId());
        assertThat(detail.getImages()).isEmpty();
        assertThat(detail.getSpecifications()).extracting(ProductDetailResponse.SpecificationGroup::getGroup)
                .containsExactly("Display", "Battery");
        assertThat(detail.getSpecifications().get(0).getItems())
                .extracting(ProductDetailResponse.SpecificationItem::getKey)
                .containsExactly("Size", "Type");
    }

    @Test
    void createProduct_rejectsMrpLowerThanPrice_andPersistsNothing() {
        long before = count("SELECT COUNT(*) FROM products");

        assertThatThrownBy(() -> productService.createProduct(newProduct().mrp(new BigDecimal("499.99")).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("MRP cannot be lower than the price");

        assertThat(count("SELECT COUNT(*) FROM products")).isEqualTo(before);
    }

    @Test
    void updateProduct_nullFieldsLeaveStoredValuesUnchanged_providedListsReplace_emptyListsClear() {
        Product seeded = productRepository.findBySkuCode("APPLE-IP15P-128").orElseThrow();
        Long id = seeded.getId();
        ProductRequest sameCore = ProductRequest.builder()
                .name(seeded.getName())
                .price(seeded.getPrice())
                .category(seeded.getCategory())
                .skuCode(seeded.getSkuCode())
                .build();
        flushAndClear();

        // 1. nothing optional supplied -> everything optional is untouched
        productService.updateProduct(id, sameCore);
        flushAndClear();
        Product unchanged = productRepository.findById(id).orElseThrow();
        assertThat(unchanged.getBrand()).isEqualTo("Apple");
        assertThat(unchanged.getMrp()).isEqualByComparingTo("1099.99");
        assertThat(unchanged.getWarranty()).isEqualTo("1 year limited warranty");
        assertThat(unchanged.getHighlights()).hasSize(5);
        assertThat(unchanged.getSpecifications()).hasSize(9);
        assertThat(unchanged.getImages()).hasSize(4);
        flushAndClear();

        // 2. provided lists replace the stored ones; provided scalars overwrite
        ProductRequest replacing = ProductRequest.builder()
                .name(seeded.getName())
                .price(seeded.getPrice())
                .category(seeded.getCategory())
                .skuCode(seeded.getSkuCode())
                .brand("Apple Inc")
                .highlights(List.of("only highlight"))
                .specifications(List.of(new ProductRequest.Specification("General", "Colour", "Black")))
                .build();
        productService.updateProduct(id, replacing);
        flushAndClear();
        assertThat(jdbc.queryForList("SELECT text FROM product_highlights WHERE product_id = ?", String.class, id))
                .containsExactly("only highlight");
        assertThat(jdbc.queryForList("SELECT spec_value FROM product_specifications WHERE product_id = ?",
                String.class, id)).containsExactly("Black");
        assertThat(jdbc.queryForObject("SELECT brand FROM products WHERE id = ?", String.class, id))
                .isEqualTo("Apple Inc");
        // mrp was null in the request -> still the seeded value
        assertThat(jdbc.queryForObject("SELECT mrp FROM products WHERE id = ?", BigDecimal.class, id))
                .isEqualByComparingTo("1099.99");
        // images are not touched by a product update
        assertThat(count("SELECT COUNT(*) FROM product_images WHERE product_id = ?", id)).isEqualTo(4);

        // 3. empty lists clear them
        ProductRequest clearing = ProductRequest.builder()
                .name(seeded.getName())
                .price(seeded.getPrice())
                .category(seeded.getCategory())
                .skuCode(seeded.getSkuCode())
                .highlights(List.of())
                .specifications(List.of())
                .build();
        productService.updateProduct(id, clearing);
        flushAndClear();
        assertThat(count("SELECT COUNT(*) FROM product_highlights WHERE product_id = ?", id)).isZero();
        assertThat(count("SELECT COUNT(*) FROM product_specifications WHERE product_id = ?", id)).isZero();
    }

    @Test
    void updateProduct_rejectsMrpLowerThanPrice() {
        Product seeded = productRepository.findBySkuCode("APPLE-IP15P-128").orElseThrow();
        ProductRequest lowMrp = ProductRequest.builder()
                .name(seeded.getName())
                .price(seeded.getPrice())
                .skuCode(seeded.getSkuCode())
                .mrp(new BigDecimal("1.00"))
                .build();

        assertThatThrownBy(() -> productService.updateProduct(seeded.getId(), lowMrp))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("MRP cannot be lower than the price");
    }

    // ------------------------------------------------------------------ delete

    @Test
    void deleteProduct_removesImagesHighlightsAndSpecifications() {
        Long id = idOf("GOPRO-H12-BLK");
        flushAndClear();

        productService.deleteProduct(id);
        flushAndClear();

        assertThat(count("SELECT COUNT(*) FROM products WHERE id = ?", id)).isZero();
        assertThat(count("SELECT COUNT(*) FROM product_images WHERE product_id = ?", id)).isZero();
        assertThat(count("SELECT COUNT(*) FROM product_highlights WHERE product_id = ?", id)).isZero();
        assertThat(count("SELECT COUNT(*) FROM product_specifications WHERE product_id = ?", id)).isZero();
    }

    // ------------------------------------------------------------------ no N+1 on list/search

    private Statistics freshStatistics() {
        flushAndClear();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        return statistics;
    }

    @Test
    void listEndpoints_doNotLoadTheLazyCollections() {
        Statistics statistics = freshStatistics();

        var page = productService.getAllProducts(PageRequest.of(0, 100));

        assertThat(page.getContent()).hasSize(100);
        assertThat(page.getContent()).allSatisfy(product -> {
            assertThat(product.getBrand()).isNotBlank();
            assertThat(product.getImageUrl()).isNotBlank();
        });
        // one page query + one count query, regardless of the page size
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(2);
        assertThat(statistics.getCollectionFetchCount()).isZero();
        assertThat(statistics.getEntityFetchCount()).isZero();
    }

    @Test
    void searchAndCategoryEndpoints_doNotLoadTheLazyCollections() {
        Statistics statistics = freshStatistics();

        var byCategory = productService.getProductsByCategory("Electronics", PageRequest.of(0, 100));
        var searched = productService.searchProducts("Monitor", "Electronics", null, null, PageRequest.of(0, 100));

        assertThat(byCategory.getContent()).hasSize(100);
        assertThat(searched.getContent()).isNotEmpty();
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(4);
        assertThat(statistics.getCollectionFetchCount()).isZero();
        assertThat(searched.getContent()).extracting(ProductResponse::getDiscountPercent).doesNotContainNull();
    }

    @Test
    void getProductDetailsById_loadsEachCollectionOnce() {
        when(inventoryClient.findStockBySkuCode(anyString())).thenReturn(Optional.empty());
        Long id = idOf("APPLE-IP15P-128");
        Statistics statistics = freshStatistics();

        productService.getProductDetailsById(id);

        // product + images + highlights + specifications
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(4);
        assertThat(statistics.getCollectionFetchCount()).isEqualTo(3);
    }

    @Test
    void replaceProductImages_keepsTheAltTextOfEachImageInOrder() {
        ProductDetailResponse result = productService.replaceProductImages(idOf("APPLE-IP15P-128"),
                images("https://cdn.example.com/a.png", "https://cdn.example.com/b.png"));

        assertThat(result.getImages()).extracting(ProductDetailResponse.Image::getUrl, ProductDetailResponse.Image::getAlt)
                .containsExactly(
                        tuple("https://cdn.example.com/a.png", "alt 1"),
                        tuple("https://cdn.example.com/b.png", "alt 2"));
    }
}
