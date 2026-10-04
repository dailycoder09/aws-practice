package com.microservices.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.product.dto.request.ProductRequest;
import com.microservices.product.dto.response.ProductResponse;
import com.microservices.product.exception.DuplicateSkuException;
import com.microservices.product.exception.ProductNotFoundException;
import com.microservices.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private ProductRequest validRequest() {
        return ProductRequest.builder()
                .name("iPhone 15 Pro")
                .price(new BigDecimal("999.99"))
                .category("Electronics")
                .skuCode("APPLE-IP15P-128")
                .build();
    }

    private ProductResponse response() {
        return ProductResponse.builder()
                .id(1L)
                .name("iPhone 15 Pro")
                .price(new BigDecimal("999.99"))
                .category("Electronics")
                .skuCode("APPLE-IP15P-128")
                .build();
    }

    @Test
    void createProduct_returns201_whenRequestIsValid() throws Exception {
        when(productService.createProduct(any(ProductRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.skuCode").value("APPLE-IP15P-128"));
    }

    @Test
    void createProduct_returns409_whenSkuAlreadyExists() throws Exception {
        when(productService.createProduct(any(ProductRequest.class)))
                .thenThrow(new DuplicateSkuException("APPLE-IP15P-128"));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict());
    }

    @Test
    void createProduct_returns400_whenNameIsBlank() throws Exception {
        ProductRequest invalid = ProductRequest.builder()
                .name("")
                .price(new BigDecimal("10.00"))
                .skuCode("ABC-123")
                .build();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void createProduct_returns400_whenPriceIsNegative() throws Exception {
        ProductRequest invalid = ProductRequest.builder()
                .name("Valid Name")
                .price(new BigDecimal("-5.00"))
                .skuCode("ABC-123")
                .build();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createProduct_returns400_whenSkuCodeHasInvalidPattern() throws Exception {
        ProductRequest invalid = ProductRequest.builder()
                .name("Valid Name")
                .price(new BigDecimal("10.00"))
                .skuCode("lowercase-not-allowed")
                .build();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProductById_returns200_whenProductExists() throws Exception {
        when(productService.getProductById(1L)).thenReturn(response());

        mockMvc.perform(get("/api/products/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getProductById_returns404_whenProductMissing() throws Exception {
        when(productService.getProductById(99L)).thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(get("/api/products/{id}", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllProducts_returns200_withPagedBody() throws Exception {
        Page<ProductResponse> page = new PageImpl<>(List.of(response()), PageRequest.of(0, 20), 1);
        when(productService.getAllProducts(any())).thenReturn(page);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].skuCode").value("APPLE-IP15P-128"));
    }

    @Test
    void updateProduct_returns200_whenRequestIsValid() throws Exception {
        when(productService.updateProduct(anyLong(), any(ProductRequest.class))).thenReturn(response());

        mockMvc.perform(put("/api/products/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk());
    }

    @Test
    void deleteProduct_returns204_whenProductExists() throws Exception {
        mockMvc.perform(delete("/api/products/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteProduct_returns404_whenProductMissing() throws Exception {
        org.mockito.Mockito.doThrow(new ProductNotFoundException(99L))
                .when(productService).deleteProduct(99L);

        mockMvc.perform(delete("/api/products/{id}", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProductBySkuCode_returns200_whenProductExists() throws Exception {
        when(productService.getProductBySkuCode("APPLE-IP15P-128")).thenReturn(response());

        mockMvc.perform(get("/api/products/sku/{skuCode}", "APPLE-IP15P-128"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skuCode").value("APPLE-IP15P-128"));
    }

    @Test
    void getProductBySkuCode_returns404_whenSkuMissing() throws Exception {
        when(productService.getProductBySkuCode("UNKNOWN")).thenThrow(new ProductNotFoundException("UNKNOWN"));

        mockMvc.perform(get("/api/products/sku/{skuCode}", "UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProductsByCategory_returns200() throws Exception {
        Page<ProductResponse> page = new PageImpl<>(List.of(response()), PageRequest.of(0, 20), 1);
        when(productService.getProductsByCategory(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/products/category/{category}", "Electronics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].category").value("Electronics"));
    }

    @Test
    void getAllCategories_returns200() throws Exception {
        when(productService.getAllCategories()).thenReturn(List.of("Electronics", "Home Appliances"));

        mockMvc.perform(get("/api/products/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Electronics"));
    }

    @Test
    void countProductsByCategory_returns200() throws Exception {
        when(productService.countProductsByCategory("Electronics")).thenReturn(9L);

        mockMvc.perform(get("/api/products/category/{category}/count", "Electronics"))
                .andExpect(status().isOk())
                .andExpect(content().string("9"));
    }

    @Test
    void existsBySkuCode_returns200() throws Exception {
        when(productService.existsBySkuCode("APPLE-IP15P-128")).thenReturn(true);

        mockMvc.perform(get("/api/products/sku/{skuCode}/exists", "APPLE-IP15P-128"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void searchProducts_returns400_whenServiceRejectsPriceRange() throws Exception {
        when(productService.searchProducts(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Minimum price cannot be greater than maximum price"));

        mockMvc.perform(get("/api/products/search")
                        .param("minPrice", "100")
                        .param("maxPrice", "50"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unmappedPath_returns404_notInternalServerError() throws Exception {
        mockMvc.perform(get("/this-route-does-not-exist"))
                .andExpect(status().isNotFound());
    }
}
