package com.microservices.product.service;

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
import com.microservices.product.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private InventoryClient inventoryClient;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;
    private ProductRequest request;
    private ProductResponse response;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("iPhone 15 Pro")
                .price(new BigDecimal("999.99"))
                .category("Electronics")
                .skuCode("APPLE-IP15P-128")
                .build();

        request = ProductRequest.builder()
                .name("iPhone 15 Pro")
                .price(new BigDecimal("999.99"))
                .category("Electronics")
                .skuCode("APPLE-IP15P-128")
                .build();

        response = ProductResponse.builder()
                .id(1L)
                .name("iPhone 15 Pro")
                .price(new BigDecimal("999.99"))
                .category("Electronics")
                .skuCode("APPLE-IP15P-128")
                .build();
    }

    @Test
    void createProduct_savesAndReturnsResponse_whenSkuIsUnique() {
        when(productRepository.existsBySkuCode(request.getSkuCode())).thenReturn(false);
        when(productMapper.toEntity(request)).thenReturn(product);
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.createProduct(request);

        assertThat(result).isEqualTo(response);
        verify(productRepository).save(product);
    }

    @Test
    void createProduct_throwsDuplicateSkuException_whenSkuAlreadyExists() {
        when(productRepository.existsBySkuCode(request.getSkuCode())).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(DuplicateSkuException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    void getProductById_returnsResponse_whenProductExists() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.getProductById(1L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getProductById_throwsProductNotFoundException_whenProductMissing() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void getProductBySkuCode_throwsProductNotFoundException_whenSkuMissing() {
        when(productRepository.findBySkuCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductBySkuCode("UNKNOWN"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void getProductById_includesStockQuantity_whenInventoryServiceHasRecord() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(response);
        when(inventoryClient.findStockBySkuCode("APPLE-IP15P-128")).thenReturn(Optional.of(42));

        ProductResponse result = productService.getProductById(1L);

        assertThat(result.getStockQuantity()).isEqualTo(42);
    }

    @Test
    void getProductById_hasNullStockQuantity_whenInventoryServiceReturnsEmpty() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(response);
        when(inventoryClient.findStockBySkuCode("APPLE-IP15P-128")).thenReturn(Optional.empty());

        ProductResponse result = productService.getProductById(1L);

        assertThat(result.getStockQuantity()).isNull();
    }

    @Test
    void getProductBySkuCode_includesStockQuantity_whenInventoryServiceHasRecord() {
        when(productRepository.findBySkuCode("APPLE-IP15P-128")).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(response);
        when(inventoryClient.findStockBySkuCode("APPLE-IP15P-128")).thenReturn(Optional.of(42));

        ProductResponse result = productService.getProductBySkuCode("APPLE-IP15P-128");

        assertThat(result.getStockQuantity()).isEqualTo(42);
    }

    @Test
    void getProductBySkuCode_hasNullStockQuantity_whenInventoryServiceReturnsEmpty() {
        when(productRepository.findBySkuCode("APPLE-IP15P-128")).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(response);
        when(inventoryClient.findStockBySkuCode("APPLE-IP15P-128")).thenReturn(Optional.empty());

        ProductResponse result = productService.getProductBySkuCode("APPLE-IP15P-128");

        assertThat(result.getStockQuantity()).isNull();
    }

    @Test
    void updateProduct_updatesAndReturnsResponse_whenSkuUnchanged() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.updateProduct(1L, request);

        assertThat(result).isEqualTo(response);
        verify(productRepository, never()).existsBySkuCode(anyString());
    }

    @Test
    void updateProduct_throwsDuplicateSkuException_whenNewSkuAlreadyTaken() {
        ProductRequest changedSkuRequest = ProductRequest.builder()
                .name("iPhone 15 Pro")
                .price(new BigDecimal("999.99"))
                .category("Electronics")
                .skuCode("NEW-SKU-999")
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsBySkuCode("NEW-SKU-999")).thenReturn(true);

        assertThatThrownBy(() -> productService.updateProduct(1L, changedSkuRequest))
                .isInstanceOf(DuplicateSkuException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    void updateProduct_throwsProductNotFoundException_whenProductMissing() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(99L, request))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void deleteProduct_deletesProduct_whenProductExists() {
        when(productRepository.existsById(1L)).thenReturn(true);

        productService.deleteProduct(1L);

        verify(productRepository).deleteById(1L);
    }

    @Test
    void deleteProduct_throwsProductNotFoundException_whenProductMissing() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ProductNotFoundException.class);

        verify(productRepository, never()).deleteById(any());
    }

    @Test
    void getProductsByPriceRange_throwsIllegalArgumentException_whenMinGreaterThanMax() {
        BigDecimal min = new BigDecimal("100");
        BigDecimal max = new BigDecimal("50");
        Pageable pageable = PageRequest.of(0, 20);

        assertThatThrownBy(() -> productService.getProductsByPriceRange(min, max, pageable))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void searchProducts_throwsIllegalArgumentException_whenMinGreaterThanMax() {
        BigDecimal min = new BigDecimal("100");
        BigDecimal max = new BigDecimal("50");
        Pageable pageable = PageRequest.of(0, 20);

        assertThatThrownBy(() -> productService.searchProducts("phone", "Electronics", min, max, pageable))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void existsBySkuCode_delegatesToRepository() {
        when(productRepository.existsBySkuCode("APPLE-IP15P-128")).thenReturn(true);

        boolean exists = productService.existsBySkuCode("APPLE-IP15P-128");

        assertThat(exists).isTrue();
    }

    @Test
    void countProductsByCategory_delegatesToRepository() {
        when(productRepository.countByCategory("Electronics")).thenReturn(5L);

        long count = productService.countProductsByCategory("Electronics");

        assertThat(count).isEqualTo(5L);
    }

    @Test
    void getAllProducts_pageable_returnsMappedPage() {
        Page<Product> page = new PageImpl<>(List.of(product));
        when(productRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(productMapper.toResponse(product)).thenReturn(response);

        Page<ProductResponse> result = productService.getAllProducts(PageRequest.of(0, 20));

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    void getAllProducts_noArgs_returnsMappedList() {
        when(productRepository.findAll()).thenReturn(List.of(product));
        when(productMapper.toResponseList(List.of(product))).thenReturn(List.of(response));

        List<ProductResponse> result = productService.getAllProducts();

        assertThat(result).containsExactly(response);
    }

    @Test
    void getProductsByCategory_returnsMappedPage() {
        Page<Product> page = new PageImpl<>(List.of(product));
        when(productRepository.findByCategory(eq("Electronics"), any(Pageable.class))).thenReturn(page);
        when(productMapper.toResponse(product)).thenReturn(response);

        Page<ProductResponse> result = productService.getProductsByCategory("Electronics", PageRequest.of(0, 20));

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    void getProductsByPriceRange_returnsMappedPage_whenRangeIsValid() {
        Page<Product> page = new PageImpl<>(List.of(product));
        BigDecimal min = new BigDecimal("500");
        BigDecimal max = new BigDecimal("1500");
        when(productRepository.findByPriceBetween(eq(min), eq(max), any(Pageable.class))).thenReturn(page);
        when(productMapper.toResponse(product)).thenReturn(response);

        Page<ProductResponse> result = productService.getProductsByPriceRange(min, max, PageRequest.of(0, 20));

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    void searchProductsByName_returnsMappedPage() {
        Page<Product> page = new PageImpl<>(List.of(product));
        when(productRepository.findByNameContainingIgnoreCase(eq("iPhone"), any(Pageable.class))).thenReturn(page);
        when(productMapper.toResponse(product)).thenReturn(response);

        Page<ProductResponse> result = productService.searchProductsByName("iPhone", PageRequest.of(0, 20));

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    void searchProducts_returnsMappedPage_whenRangeIsValid() {
        Page<Product> page = new PageImpl<>(List.of(product));
        BigDecimal min = new BigDecimal("500");
        BigDecimal max = new BigDecimal("1500");
        when(productRepository.searchProducts(eq("iPhone"), eq("Electronics"), eq(min), eq(max), any(Pageable.class)))
                .thenReturn(page);
        when(productMapper.toResponse(product)).thenReturn(response);

        Page<ProductResponse> result = productService.searchProducts("iPhone", "Electronics", min, max, PageRequest.of(0, 20));

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    void getAllCategories_delegatesToRepository() {
        when(productRepository.findAllDistinctCategories()).thenReturn(List.of("Electronics", "Home Appliances"));

        List<String> categories = productService.getAllCategories();

        assertThat(categories).containsExactly("Electronics", "Home Appliances");
    }

    // ------------------------------------------------------------------ product details

    private ProductDetailResponse detailResponse() {
        return ProductDetailResponse.builder()
                .id(1L)
                .name("iPhone 15 Pro")
                .price(new BigDecimal("999.99"))
                .skuCode("APPLE-IP15P-128")
                .build();
    }

    @Test
    void getProductDetailsById_includesStockQuantity_whenInventoryServiceHasRecord() {
        ProductDetailResponse detail = detailResponse();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productMapper.toDetailResponse(product)).thenReturn(detail);
        when(inventoryClient.findStockBySkuCode("APPLE-IP15P-128")).thenReturn(Optional.of(42));

        ProductDetailResponse result = productService.getProductDetailsById(1L);

        assertThat(result).isSameAs(detail);
        assertThat(result.getStockQuantity()).isEqualTo(42);
    }

    @Test
    void getProductDetailsById_hasNullStockQuantity_whenInventoryServiceHasNoRecordOrIsDown() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productMapper.toDetailResponse(product)).thenReturn(detailResponse());
        when(inventoryClient.findStockBySkuCode("APPLE-IP15P-128")).thenReturn(Optional.empty());

        ProductDetailResponse result = productService.getProductDetailsById(1L);

        assertThat(result.getStockQuantity()).isNull();
        assertThat(result.getSkuCode()).isEqualTo("APPLE-IP15P-128");
    }

    @Test
    void getProductDetailsById_throwsProductNotFoundException_whenProductMissing() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductDetailsById(99L))
                .isInstanceOf(ProductNotFoundException.class);

        verifyNoInteractions(inventoryClient);
    }

    @Test
    void getProductDetailsBySkuCode_includesStockQuantity_whenInventoryServiceHasRecord() {
        ProductDetailResponse detail = detailResponse();
        when(productRepository.findBySkuCode("APPLE-IP15P-128")).thenReturn(Optional.of(product));
        when(productMapper.toDetailResponse(product)).thenReturn(detail);
        when(inventoryClient.findStockBySkuCode("APPLE-IP15P-128")).thenReturn(Optional.of(7));

        ProductDetailResponse result = productService.getProductDetailsBySkuCode("APPLE-IP15P-128");

        assertThat(result.getStockQuantity()).isEqualTo(7);
    }

    @Test
    void getProductDetailsBySkuCode_hasNullStockQuantity_whenInventoryServiceReturnsEmpty() {
        when(productRepository.findBySkuCode("APPLE-IP15P-128")).thenReturn(Optional.of(product));
        when(productMapper.toDetailResponse(product)).thenReturn(detailResponse());
        when(inventoryClient.findStockBySkuCode("APPLE-IP15P-128")).thenReturn(Optional.empty());

        ProductDetailResponse result = productService.getProductDetailsBySkuCode("APPLE-IP15P-128");

        assertThat(result.getStockQuantity()).isNull();
    }

    @Test
    void getProductDetailsBySkuCode_throwsProductNotFoundException_whenSkuMissing() {
        when(productRepository.findBySkuCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductDetailsBySkuCode("UNKNOWN"))
                .isInstanceOf(ProductNotFoundException.class);
    }

    // ------------------------------------------------------------------ replace images

    private ProductImagesRequest imagesRequest(String... urls) {
        List<ProductImagesRequest.Image> images = new ArrayList<>();
        for (String url : urls) {
            images.add(new ProductImagesRequest.Image(url, "alt for " + url));
        }
        return new ProductImagesRequest(images);
    }

    @Test
    void replaceProductImages_replacesListInOrderAndSetsPrimaryImageUrl() {
        product.getImages().add(ProductImage.builder().product(product).url("https://old/1.png").sortOrder(1).build());
        product.getImages().add(ProductImage.builder().product(product).url("https://old/2.png").sortOrder(2).build());
        ProductDetailResponse detail = detailResponse();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toDetailResponse(product)).thenReturn(detail);
        when(inventoryClient.findStockBySkuCode("APPLE-IP15P-128")).thenReturn(Optional.of(5));

        ProductDetailResponse result = productService.replaceProductImages(1L,
                imagesRequest("https://new/a.png", "https://new/b.png", "https://new/c.png"));

        assertThat(product.getImageUrl()).isEqualTo("https://new/a.png");
        assertThat(product.getImages())
                .extracting(ProductImage::getUrl, ProductImage::getSortOrder, ProductImage::getAlt)
                .containsExactly(
                        tuple("https://new/a.png", 1, "alt for https://new/a.png"),
                        tuple("https://new/b.png", 2, "alt for https://new/b.png"),
                        tuple("https://new/c.png", 3, "alt for https://new/c.png"));
        assertThat(product.getImages()).allSatisfy(image -> assertThat(image.getProduct()).isSameAs(product));
        assertThat(result).isSameAs(detail);
        assertThat(result.getStockQuantity()).isEqualTo(5);
    }

    @Test
    void replaceProductImages_flushesTheDeletesBeforeInsertingTheNewRows() {
        product.getImages().add(ProductImage.builder().product(product).url("https://old/1.png").sortOrder(1).build());
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toDetailResponse(product)).thenReturn(detailResponse());
        when(inventoryClient.findStockBySkuCode(anyString())).thenReturn(Optional.empty());

        productService.replaceProductImages(1L, imagesRequest("https://new/a.png"));

        // unique (product_id, sort_order): the old row must be gone from the DB before position 1 is re-inserted
        org.mockito.InOrder inOrder = inOrder(productRepository);
        inOrder.verify(productRepository).flush();
        inOrder.verify(productRepository).save(product);
    }

    @Test
    void replaceProductImages_clearsImagesAndPrimaryImageUrl_whenListIsEmpty() {
        product.setImageUrl("https://old/1.png");
        product.getImages().add(ProductImage.builder().product(product).url("https://old/1.png").sortOrder(1).build());
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toDetailResponse(product)).thenReturn(detailResponse());
        when(inventoryClient.findStockBySkuCode(anyString())).thenReturn(Optional.empty());

        productService.replaceProductImages(1L, imagesRequest());

        assertThat(product.getImages()).isEmpty();
        assertThat(product.getImageUrl()).isNull();
        verify(productRepository).save(product);
    }

    @Test
    void replaceProductImages_throwsProductNotFoundException_whenProductMissing() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.replaceProductImages(99L, imagesRequest("https://new/a.png")))
                .isInstanceOf(ProductNotFoundException.class);

        verify(productRepository, never()).save(any());
    }

    @Test
    void replaceProductImages_throwsIllegalArgumentException_whenImagesIsNull() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.replaceProductImages(1L, new ProductImagesRequest(null)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(productRepository, never()).save(any());
    }

    // ------------------------------------------------------------------ create/update with detail fields

    @Test
    void createProduct_storesHighlightsAndSpecificationsInOrder() {
        ProductRequest withDetails = ProductRequest.builder()
                .name("iPhone 15 Pro")
                .price(new BigDecimal("999.99"))
                .mrp(new BigDecimal("1099.99"))
                .skuCode("APPLE-IP15P-128")
                .highlights(List.of("A17 Pro chip", "USB-C"))
                .specifications(List.of(
                        new ProductRequest.Specification("Display", "Size", "6.1 in"),
                        new ProductRequest.Specification("Performance", "Chip", "A17 Pro")))
                .build();
        when(productRepository.existsBySkuCode("APPLE-IP15P-128")).thenReturn(false);
        when(productMapper.toEntity(withDetails)).thenReturn(product);
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.createProduct(withDetails);

        assertThat(result).isEqualTo(response);
        assertThat(product.getHighlights())
                .extracting(ProductHighlight::getText, ProductHighlight::getSortOrder)
                .containsExactly(tuple("A17 Pro chip", 1), tuple("USB-C", 2));
        assertThat(product.getSpecifications())
                .extracting(ProductSpecification::getGroupName, ProductSpecification::getSpecKey,
                        ProductSpecification::getSpecValue, ProductSpecification::getSortOrder)
                .containsExactly(
                        tuple("Display", "Size", "6.1 in", 1),
                        tuple("Performance", "Chip", "A17 Pro", 2));
        assertThat(product.getHighlights()).allSatisfy(h -> assertThat(h.getProduct()).isSameAs(product));
        assertThat(product.getSpecifications()).allSatisfy(s -> assertThat(s.getProduct()).isSameAs(product));
    }

    @Test
    void createProduct_leavesCollectionsEmpty_whenRequestHasNoDetailLists() {
        when(productRepository.existsBySkuCode(request.getSkuCode())).thenReturn(false);
        when(productMapper.toEntity(request)).thenReturn(product);
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);

        productService.createProduct(request);

        assertThat(product.getHighlights()).isEmpty();
        assertThat(product.getSpecifications()).isEmpty();
    }

    @Test
    void createProduct_throwsIllegalArgumentException_whenMrpIsLowerThanPrice() {
        ProductRequest lowMrp = ProductRequest.builder()
                .name("iPhone 15 Pro")
                .price(new BigDecimal("999.99"))
                .mrp(new BigDecimal("899.99"))
                .skuCode("APPLE-IP15P-128")
                .build();

        assertThatThrownBy(() -> productService.createProduct(lowMrp))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("MRP cannot be lower than the price");

        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_accepts_whenMrpEqualsPrice() {
        ProductRequest equalMrp = ProductRequest.builder()
                .name("iPhone 15 Pro")
                .price(new BigDecimal("999.99"))
                .mrp(new BigDecimal("999.990"))
                .skuCode("APPLE-IP15P-128")
                .build();
        when(productRepository.existsBySkuCode("APPLE-IP15P-128")).thenReturn(false);
        when(productMapper.toEntity(equalMrp)).thenReturn(product);
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);

        assertThat(productService.createProduct(equalMrp)).isEqualTo(response);
    }

    @Test
    void updateProduct_replacesHighlightsAndSpecifications_whenProvided() {
        product.getHighlights().add(ProductHighlight.builder().product(product).text("old").sortOrder(1).build());
        product.getSpecifications().add(ProductSpecification.builder().product(product)
                .groupName("Old").specKey("k").specValue("v").sortOrder(1).build());
        ProductRequest replacing = ProductRequest.builder()
                .name("iPhone 15 Pro")
                .price(new BigDecimal("999.99"))
                .skuCode("APPLE-IP15P-128")
                .highlights(List.of("new one", "new two"))
                .specifications(List.of(new ProductRequest.Specification("Display", "Size", "6.1 in")))
                .build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);

        productService.updateProduct(1L, replacing);

        assertThat(product.getHighlights()).extracting(ProductHighlight::getText)
                .containsExactly("new one", "new two");
        assertThat(product.getSpecifications()).extracting(ProductSpecification::getGroupName)
                .containsExactly("Display");
    }

    @Test
    void updateProduct_leavesCollectionsUnchanged_whenListsAreNull() {
        product.getHighlights().add(ProductHighlight.builder().product(product).text("keep").sortOrder(1).build());
        product.getSpecifications().add(ProductSpecification.builder().product(product)
                .groupName("Keep").specKey("k").specValue("v").sortOrder(1).build());
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);

        productService.updateProduct(1L, request);

        assertThat(product.getHighlights()).extracting(ProductHighlight::getText).containsExactly("keep");
        assertThat(product.getSpecifications()).extracting(ProductSpecification::getGroupName).containsExactly("Keep");
    }

    @Test
    void updateProduct_clearsCollections_whenListsAreEmpty() {
        product.getHighlights().add(ProductHighlight.builder().product(product).text("gone").sortOrder(1).build());
        product.getSpecifications().add(ProductSpecification.builder().product(product)
                .groupName("Gone").specKey("k").specValue("v").sortOrder(1).build());
        ProductRequest clearing = ProductRequest.builder()
                .name("iPhone 15 Pro")
                .price(new BigDecimal("999.99"))
                .skuCode("APPLE-IP15P-128")
                .highlights(List.of())
                .specifications(List.of())
                .build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);

        productService.updateProduct(1L, clearing);

        assertThat(product.getHighlights()).isEmpty();
        assertThat(product.getSpecifications()).isEmpty();
    }

    @Test
    void updateProduct_throwsIllegalArgumentException_whenMrpIsLowerThanPrice() {
        ProductRequest lowMrp = ProductRequest.builder()
                .name("iPhone 15 Pro")
                .price(new BigDecimal("999.99"))
                .mrp(new BigDecimal("500.00"))
                .skuCode("APPLE-IP15P-128")
                .build();

        assertThatThrownBy(() -> productService.updateProduct(1L, lowMrp))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("MRP cannot be lower than the price");

        verify(productRepository, never()).save(any());
    }

    @Test
    void updateProduct_keepsExistingDuplicateSkuCheck_whenDetailFieldsAreSent() {
        ProductRequest changedSku = ProductRequest.builder()
                .name("iPhone 15 Pro")
                .price(new BigDecimal("999.99"))
                .mrp(new BigDecimal("1099.99"))
                .skuCode("NEW-SKU-999")
                .highlights(List.of("x"))
                .build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsBySkuCode("NEW-SKU-999")).thenReturn(true);

        assertThatThrownBy(() -> productService.updateProduct(1L, changedSku))
                .isInstanceOf(DuplicateSkuException.class);

        assertThat(product.getHighlights()).isEmpty();
        verify(productRepository, never()).save(any());
    }
}
