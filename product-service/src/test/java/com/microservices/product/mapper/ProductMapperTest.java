package com.microservices.product.mapper;

import com.microservices.product.dto.request.ProductRequest;
import com.microservices.product.dto.response.ProductDetailResponse;
import com.microservices.product.dto.response.ProductResponse;
import com.microservices.product.model.Product;
import com.microservices.product.model.ProductHighlight;
import com.microservices.product.model.ProductImage;
import com.microservices.product.model.ProductSpecification;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMapperTest {

    private final ProductMapper mapper = new ProductMapperImpl();

    /**
     * A list that fails the test on any access - stands in for a lazy collection that
     * must not be initialised.
     */
    private static <T> List<T> untouchable() {
        return new AbstractList<>() {
            @Override
            public T get(int index) {
                throw new AssertionError("lazy collection must not be touched");
            }

            @Override
            public int size() {
                throw new AssertionError("lazy collection must not be touched");
            }
        };
    }

    private Product product() {
        return Product.builder()
                .id(7L)
                .name("iPhone 15 Pro")
                .description("Flagship phone")
                .price(new BigDecimal("999.99"))
                .mrp(new BigDecimal("1099.99"))
                .category("Electronics")
                .skuCode("APPLE-IP15P-128")
                .brand("Apple")
                .warranty("1 year limited warranty")
                .seller("Orbit Electronics Retail")
                .imageUrl("https://placehold.co/800x800/172740/E6EDF7/png?text=APPLE-IP15P-128+1")
                .build();
    }

    // ---------------------------------------------------------------- toResponse

    @Test
    void toResponse_mapsScalarFieldsAndDerivedDiscount() {
        ProductResponse response = mapper.toResponse(product());

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getName()).isEqualTo("iPhone 15 Pro");
        assertThat(response.getBrand()).isEqualTo("Apple");
        assertThat(response.getMrp()).isEqualByComparingTo("1099.99");
        assertThat(response.getDiscountPercent()).isEqualTo(9);
        assertThat(response.getImageUrl()).endsWith("APPLE-IP15P-128+1");
        assertThat(response.getStockQuantity()).isNull();
    }

    @Test
    void toResponse_hasNullDiscount_whenMrpIsMissingOrNotAbovePrice() {
        Product noMrp = product();
        noMrp.setMrp(null);
        Product sameMrp = product();
        sameMrp.setMrp(sameMrp.getPrice());

        assertThat(mapper.toResponse(noMrp).getDiscountPercent()).isNull();
        assertThat(mapper.toResponse(sameMrp).getDiscountPercent()).isNull();
    }

    @Test
    void toResponse_neverTouchesLazyCollections() {
        Product product = product();
        product.setImages(untouchable());
        product.setHighlights(untouchable());
        product.setSpecifications(untouchable());

        ProductResponse response = mapper.toResponse(product);
        List<ProductResponse> responses = mapper.toResponseList(List.of(product));

        assertThat(response.getSkuCode()).isEqualTo("APPLE-IP15P-128");
        assertThat(responses).hasSize(1);
    }

    // ---------------------------------------------------------------- toEntity / update

    @Test
    void toEntity_mapsNewScalarFields_andLeavesCollectionsEmpty() {
        ProductRequest request = ProductRequest.builder()
                .name("iPhone 15 Pro")
                .price(new BigDecimal("999.99"))
                .skuCode("APPLE-IP15P-128")
                .brand("Apple")
                .mrp(new BigDecimal("1099.99"))
                .warranty("1 year")
                .seller("Orbit")
                .highlights(List.of("A17 Pro"))
                .specifications(List.of(new ProductRequest.Specification("Display", "Size", "6.1 in")))
                .build();

        Product entity = mapper.toEntity(request);

        assertThat(entity.getBrand()).isEqualTo("Apple");
        assertThat(entity.getMrp()).isEqualByComparingTo("1099.99");
        assertThat(entity.getWarranty()).isEqualTo("1 year");
        assertThat(entity.getSeller()).isEqualTo("Orbit");
        assertThat(entity.getImageUrl()).isNull();
        // collections are handled by the service, not by MapStruct
        assertThat(entity.getImages()).isEmpty();
        assertThat(entity.getHighlights()).isEmpty();
        assertThat(entity.getSpecifications()).isEmpty();
    }

    @Test
    void updateEntityFromRequest_overwritesProvidedFields_andKeepsNullOnes() {
        Product existing = product();
        ProductRequest request = ProductRequest.builder()
                .name("iPhone 15 Pro Max")
                .price(new BigDecimal("1199.99"))
                .skuCode("APPLE-IP15P-128")
                .brand("Apple Inc")
                // mrp, warranty, seller left null -> unchanged
                .build();

        mapper.updateEntityFromRequest(request, existing);

        assertThat(existing.getName()).isEqualTo("iPhone 15 Pro Max");
        assertThat(existing.getBrand()).isEqualTo("Apple Inc");
        assertThat(existing.getMrp()).isEqualByComparingTo("1099.99");
        assertThat(existing.getWarranty()).isEqualTo("1 year limited warranty");
        assertThat(existing.getSeller()).isEqualTo("Orbit Electronics Retail");
        assertThat(existing.getImageUrl()).endsWith("APPLE-IP15P-128+1");
    }

    @Test
    void updateEntityFromRequest_doesNotTouchCollections() {
        Product existing = product();
        existing.setImages(untouchable());
        existing.setHighlights(untouchable());
        existing.setSpecifications(untouchable());
        ProductRequest request = ProductRequest.builder()
                .name("iPhone 15 Pro")
                .price(new BigDecimal("999.99"))
                .skuCode("APPLE-IP15P-128")
                .highlights(List.of("ignored by the mapper"))
                .build();

        mapper.updateEntityFromRequest(request, existing);

        assertThat(existing.getName()).isEqualTo("iPhone 15 Pro");
    }

    // ---------------------------------------------------------------- toDetailResponse

    private ProductImage image(Product owner, int order, String url, String alt) {
        return ProductImage.builder().product(owner).sortOrder(order).url(url).alt(alt).build();
    }

    private ProductSpecification spec(Product owner, int order, String group, String key, String value) {
        return ProductSpecification.builder().product(owner).sortOrder(order)
                .groupName(group).specKey(key).specValue(value).build();
    }

    @Test
    void toDetailResponse_mapsScalarsImagesHighlightsAndGroupedSpecifications() {
        Product product = product();
        product.getImages().add(image(product, 1, "https://img/1.png", "front"));
        product.getImages().add(image(product, 2, "https://img/2.png", null));
        product.getHighlights().add(ProductHighlight.builder().product(product).sortOrder(1).text("A17 Pro").build());
        product.getHighlights().add(ProductHighlight.builder().product(product).sortOrder(2).text("USB-C").build());
        product.getSpecifications().add(spec(product, 1, "Display", "Size", "6.1 in"));
        product.getSpecifications().add(spec(product, 2, "Performance", "Chip", "A17 Pro"));
        product.getSpecifications().add(spec(product, 3, "Display", "Refresh rate", "120 Hz"));

        ProductDetailResponse detail = mapper.toDetailResponse(product);

        assertThat(detail.getId()).isEqualTo(7L);
        assertThat(detail.getBrand()).isEqualTo("Apple");
        assertThat(detail.getMrp()).isEqualByComparingTo("1099.99");
        assertThat(detail.getDiscountPercent()).isEqualTo(9);
        assertThat(detail.getWarranty()).isEqualTo("1 year limited warranty");
        assertThat(detail.getSeller()).isEqualTo("Orbit Electronics Retail");
        assertThat(detail.getStockQuantity()).isNull();
        assertThat(detail.getImages()).containsExactly(
                new ProductDetailResponse.Image("https://img/1.png", "front"),
                new ProductDetailResponse.Image("https://img/2.png", null));
        assertThat(detail.getHighlights()).containsExactly("A17 Pro", "USB-C");

        // groups in order of first appearance, items in sort order within the group
        assertThat(detail.getSpecifications()).extracting(ProductDetailResponse.SpecificationGroup::getGroup)
                .containsExactly("Display", "Performance");
        assertThat(detail.getSpecifications().get(0).getItems()).containsExactly(
                new ProductDetailResponse.SpecificationItem("Size", "6.1 in"),
                new ProductDetailResponse.SpecificationItem("Refresh rate", "120 Hz"));
        assertThat(detail.getSpecifications().get(1).getItems()).containsExactly(
                new ProductDetailResponse.SpecificationItem("Chip", "A17 Pro"));
    }

    @Test
    void toDetailResponse_ordersChildrenBySortOrder_evenWhenListIsOutOfOrder() {
        Product product = product();
        product.getImages().add(image(product, 2, "https://img/2.png", null));
        product.getImages().add(image(product, 1, "https://img/1.png", null));
        product.getHighlights().add(ProductHighlight.builder().product(product).sortOrder(2).text("second").build());
        product.getHighlights().add(ProductHighlight.builder().product(product).sortOrder(1).text("first").build());
        product.getSpecifications().add(spec(product, 2, "B", "k2", "v2"));
        product.getSpecifications().add(spec(product, 1, "A", "k1", "v1"));

        ProductDetailResponse detail = mapper.toDetailResponse(product);

        assertThat(detail.getImages()).extracting(ProductDetailResponse.Image::getUrl)
                .containsExactly("https://img/1.png", "https://img/2.png");
        assertThat(detail.getHighlights()).containsExactly("first", "second");
        assertThat(detail.getSpecifications()).extracting(ProductDetailResponse.SpecificationGroup::getGroup)
                .containsExactly("A", "B");
    }

    @Test
    void toDetailResponse_returnsEmptyLists_whenProductHasNoChildren() {
        ProductDetailResponse detail = mapper.toDetailResponse(product());

        assertThat(detail.getImages()).isEmpty();
        assertThat(detail.getHighlights()).isEmpty();
        assertThat(detail.getSpecifications()).isEmpty();
    }

    @Test
    void toDetailResponse_returnsEmptyLists_whenCollectionsAreNull() {
        Product product = product();
        product.setImages(null);
        product.setHighlights(null);
        product.setSpecifications(null);

        ProductDetailResponse detail = mapper.toDetailResponse(product);

        assertThat(detail.getImages()).isEqualTo(new ArrayList<>());
        assertThat(detail.getHighlights()).isEmpty();
        assertThat(detail.getSpecifications()).isEmpty();
    }
}
