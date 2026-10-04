package com.microservices.product.service;

import com.microservices.product.client.InventoryClient;
import com.microservices.product.dto.request.ProductRequest;
import com.microservices.product.dto.response.ProductResponse;
import com.microservices.product.exception.DuplicateSkuException;
import com.microservices.product.exception.ProductNotFoundException;
import com.microservices.product.mapper.ProductMapper;
import com.microservices.product.model.Product;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
}
