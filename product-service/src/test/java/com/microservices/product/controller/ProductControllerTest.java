package com.microservices.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.product.dto.request.ProductImagesRequest;
import com.microservices.product.dto.request.ProductRequest;
import com.microservices.product.dto.response.ProductDetailResponse;
import com.microservices.product.dto.response.ProductResponse;
import com.microservices.product.exception.DuplicateSkuException;
import com.microservices.product.exception.ProductNotFoundException;
import com.microservices.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

    // ------------------------------------------------------------------ product details

    private ProductDetailResponse detailResponse() {
        return ProductDetailResponse.builder()
                .id(1L)
                .name("iPhone 15 Pro")
                .description("Flagship phone")
                .price(new BigDecimal("999.99"))
                .mrp(new BigDecimal("1099.99"))
                .discountPercent(9)
                .category("Electronics")
                .brand("Apple")
                .skuCode("APPLE-IP15P-128")
                .warranty("1 year limited warranty")
                .seller("Orbit Electronics Retail")
                .images(List.of(
                        new ProductDetailResponse.Image("https://placehold.co/800x800/172740/E6EDF7/png?text=APPLE-IP15P-128+1",
                                "iPhone 15 Pro - view 1"),
                        new ProductDetailResponse.Image("https://placehold.co/800x800/1D3050/E6EDF7/png?text=APPLE-IP15P-128+2",
                                "iPhone 15 Pro - view 2")))
                .highlights(List.of("A17 Pro chip", "USB-C"))
                .specifications(List.of(
                        new ProductDetailResponse.SpecificationGroup("Display", List.of(
                                new ProductDetailResponse.SpecificationItem("Screen Size", "6.1 inches"),
                                new ProductDetailResponse.SpecificationItem("Refresh Rate", "120 Hz"))),
                        new ProductDetailResponse.SpecificationGroup("Performance", List.of(
                                new ProductDetailResponse.SpecificationItem("Chip", "A17 Pro")))))
                .createdAt(LocalDateTime.of(2026, 1, 2, 3, 4, 5))
                .updatedAt(LocalDateTime.of(2026, 2, 3, 4, 5, 6))
                .build();
    }

    @Test
    void getProductDetailsById_returns200_withFullDetailShape() throws Exception {
        when(productService.getProductDetailsById(1L)).thenReturn(detailResponse());

        mockMvc.perform(get("/api/products/{id}/details", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.brand").value("Apple"))
                .andExpect(jsonPath("$.mrp").value(1099.99))
                .andExpect(jsonPath("$.discountPercent").value(9))
                .andExpect(jsonPath("$.warranty").value("1 year limited warranty"))
                .andExpect(jsonPath("$.seller").value("Orbit Electronics Retail"))
                .andExpect(jsonPath("$.images.length()").value(2))
                .andExpect(jsonPath("$.images[0].url").value(
                        "https://placehold.co/800x800/172740/E6EDF7/png?text=APPLE-IP15P-128+1"))
                .andExpect(jsonPath("$.images[1].alt").value("iPhone 15 Pro - view 2"))
                .andExpect(jsonPath("$.highlights[0]").value("A17 Pro chip"))
                .andExpect(jsonPath("$.highlights[1]").value("USB-C"))
                .andExpect(jsonPath("$.specifications.length()").value(2))
                .andExpect(jsonPath("$.specifications[0].group").value("Display"))
                .andExpect(jsonPath("$.specifications[0].items[0].key").value("Screen Size"))
                .andExpect(jsonPath("$.specifications[0].items[0].value").value("6.1 inches"))
                .andExpect(jsonPath("$.specifications[0].items[1].key").value("Refresh Rate"))
                .andExpect(jsonPath("$.specifications[1].group").value("Performance"))
                .andExpect(jsonPath("$.specifications[1].items[0].value").value("A17 Pro"))
                .andExpect(jsonPath("$.createdAt").value("2026-01-02T03:04:05"))
                .andExpect(jsonPath("$.updatedAt").value("2026-02-03T04:05:06"));
    }

    @Test
    void getProductDetailsById_alwaysSerializesStockQuantity_evenWhenNull() throws Exception {
        when(productService.getProductDetailsById(1L)).thenReturn(detailResponse());

        mockMvc.perform(get("/api/products/{id}/details", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasKey("stockQuantity")))
                .andExpect(jsonPath("$.stockQuantity").value(nullValue()));
    }

    @Test
    void getProductDetailsById_includesStockQuantity_whenPresent() throws Exception {
        ProductDetailResponse detail = detailResponse();
        detail.setStockQuantity(42);
        when(productService.getProductDetailsById(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/products/{id}/details", 1L))
                .andExpect(jsonPath("$.stockQuantity").value(42));
    }

    @Test
    void getProductDetailsById_omitsNullOptionalFields() throws Exception {
        ProductDetailResponse bare = ProductDetailResponse.builder()
                .id(2L).name("Plain").price(new BigDecimal("5.00")).skuCode("PLAIN-1")
                .images(List.of()).highlights(List.of()).specifications(List.of())
                .build();
        when(productService.getProductDetailsById(2L)).thenReturn(bare);

        mockMvc.perform(get("/api/products/{id}/details", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(hasKey("brand"))))
                .andExpect(jsonPath("$", not(hasKey("mrp"))))
                .andExpect(jsonPath("$", not(hasKey("discountPercent"))))
                .andExpect(jsonPath("$.images").isEmpty())
                .andExpect(jsonPath("$.highlights").isEmpty())
                .andExpect(jsonPath("$.specifications").isEmpty());
    }

    @Test
    void getProductDetailsById_returns404_whenProductMissing() throws Exception {
        when(productService.getProductDetailsById(99L)).thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(get("/api/products/{id}/details", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Product Not Found"));
    }

    @Test
    void getProductDetailsBySkuCode_returns200_withFullDetailShape() throws Exception {
        when(productService.getProductDetailsBySkuCode("APPLE-IP15P-128")).thenReturn(detailResponse());

        mockMvc.perform(get("/api/products/sku/{skuCode}/details", "APPLE-IP15P-128"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skuCode").value("APPLE-IP15P-128"))
                .andExpect(jsonPath("$.images.length()").value(2))
                .andExpect(jsonPath("$.specifications[0].items.length()").value(2));
    }

    @Test
    void getProductDetailsBySkuCode_returns404_whenSkuMissing() throws Exception {
        when(productService.getProductDetailsBySkuCode("UNKNOWN")).thenThrow(new ProductNotFoundException("UNKNOWN"));

        mockMvc.perform(get("/api/products/sku/{skuCode}/details", "UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    // The literal /details suffix must not be swallowed by the existing /{id} and /sku/{skuCode} mappings

    @Test
    void detailsPath_routesToDetailsHandler_notToGetProductById() throws Exception {
        when(productService.getProductDetailsById(1L)).thenReturn(detailResponse());

        mockMvc.perform(get("/api/products/1/details")).andExpect(status().isOk());

        verify(productService).getProductDetailsById(1L);
        verify(productService, never()).getProductById(anyLong());
    }

    @Test
    void plainIdPath_stillRoutesToGetProductById_notToDetails() throws Exception {
        when(productService.getProductById(1L)).thenReturn(response());

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skuCode").value("APPLE-IP15P-128"))
                .andExpect(jsonPath("$", not(hasKey("images"))));

        verify(productService).getProductById(1L);
        verify(productService, never()).getProductDetailsById(anyLong());
    }

    @Test
    void skuDetailsPath_routesToSkuDetailsHandler_notToSkuOrExists() throws Exception {
        when(productService.getProductDetailsBySkuCode("APPLE-IP15P-128")).thenReturn(detailResponse());

        mockMvc.perform(get("/api/products/sku/APPLE-IP15P-128/details")).andExpect(status().isOk());

        verify(productService).getProductDetailsBySkuCode("APPLE-IP15P-128");
        verify(productService, never()).getProductBySkuCode(anyString());
        verify(productService, never()).existsBySkuCode(anyString());
    }

    @Test
    void plainSkuAndExistsPaths_stillRouteToTheirOwnHandlers() throws Exception {
        when(productService.getProductBySkuCode("APPLE-IP15P-128")).thenReturn(response());
        when(productService.existsBySkuCode("APPLE-IP15P-128")).thenReturn(true);

        mockMvc.perform(get("/api/products/sku/APPLE-IP15P-128"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skuCode").value("APPLE-IP15P-128"));
        mockMvc.perform(get("/api/products/sku/APPLE-IP15P-128/exists"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(productService, never()).getProductDetailsBySkuCode(anyString());
    }

    @Test
    void skuPathWithLiteralDetailsAsTheSku_isStillLookedUpAsASku() throws Exception {
        // "/sku/details" also looks like "/{id}/details" with id = "sku"; the details-by-id mapping only
        // accepts digits, so this stays a plain SKU lookup (a clean 404 for an unknown SKU, not a 500)
        when(productService.getProductBySkuCode("details")).thenThrow(new ProductNotFoundException("details"));

        mockMvc.perform(get("/api/products/sku/details")).andExpect(status().isNotFound());

        verify(productService).getProductBySkuCode("details");
        verify(productService, never()).getProductDetailsById(anyLong());
    }

    @Test
    void detailsPathWithNonNumericId_isNotRoutedToTheDetailsHandler() throws Exception {
        mockMvc.perform(get("/api/products/abc/details")).andExpect(status().isNotFound());

        verify(productService, never()).getProductDetailsById(anyLong());
    }

    @Test
    void literalPaths_categoriesAndSearch_areNotCapturedByIdMappings() throws Exception {
        when(productService.getAllCategories()).thenReturn(List.of("Electronics"));
        when(productService.searchProducts(any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(response()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/products/categories")).andExpect(status().isOk());
        mockMvc.perform(get("/api/products/search")).andExpect(status().isOk());

        verify(productService, never()).getProductById(anyLong());
    }

    @Test
    void getAllProducts_returnsLightFieldsOnly_noImagesOrSpecificationsArrays() throws Exception {
        ProductResponse light = ProductResponse.builder()
                .id(1L).name("iPhone 15 Pro").price(new BigDecimal("999.99")).skuCode("APPLE-IP15P-128")
                .brand("Apple").mrp(new BigDecimal("1099.99")).discountPercent(9)
                .imageUrl("https://placehold.co/800x800/172740/E6EDF7/png?text=APPLE-IP15P-128+1")
                .build();
        when(productService.getAllProducts(any()))
                .thenReturn(new PageImpl<>(List.of(light), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].brand").value("Apple"))
                .andExpect(jsonPath("$.content[0].mrp").value(1099.99))
                .andExpect(jsonPath("$.content[0].discountPercent").value(9))
                .andExpect(jsonPath("$.content[0].imageUrl").exists())
                .andExpect(jsonPath("$.content[0]", not(hasKey("images"))))
                .andExpect(jsonPath("$.content[0]", not(hasKey("highlights"))))
                .andExpect(jsonPath("$.content[0]", not(hasKey("specifications"))));
    }

    // ------------------------------------------------------------------ PUT /{id}/images

    private String imagesBody(String... urls) throws Exception {
        List<ProductImagesRequest.Image> images = new ArrayList<>();
        for (String url : urls) {
            images.add(new ProductImagesRequest.Image(url, "alt"));
        }
        return objectMapper.writeValueAsString(new ProductImagesRequest(images));
    }

    @Test
    void replaceProductImages_returns200_andPassesTheListToTheService() throws Exception {
        when(productService.replaceProductImages(eq(1L), any(ProductImagesRequest.class))).thenReturn(detailResponse());

        mockMvc.perform(put("/api/products/{id}/images", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(imagesBody("https://cdn.example.com/a.png", "http://cdn.example.com/b.jpg")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images.length()").value(2))
                .andExpect(jsonPath("$.skuCode").value("APPLE-IP15P-128"));

        ArgumentCaptor<ProductImagesRequest> captor = ArgumentCaptor.forClass(ProductImagesRequest.class);
        verify(productService).replaceProductImages(eq(1L), captor.capture());
        assertThat(captor.getValue().getImages())
                .extracting(ProductImagesRequest.Image::getUrl)
                .containsExactly("https://cdn.example.com/a.png", "http://cdn.example.com/b.jpg");
    }

    @Test
    void replaceProductImages_returns200_forEmptyList() throws Exception {
        when(productService.replaceProductImages(eq(1L), any(ProductImagesRequest.class))).thenReturn(detailResponse());

        mockMvc.perform(put("/api/products/{id}/images", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"images\":[]}"))
                .andExpect(status().isOk());
    }

    @Test
    void replaceProductImages_returns200_forExactlyTenImages() throws Exception {
        when(productService.replaceProductImages(eq(1L), any(ProductImagesRequest.class))).thenReturn(detailResponse());
        String[] urls = new String[10];
        for (int i = 0; i < urls.length; i++) {
            urls[i] = "https://cdn.example.com/" + i + ".png";
        }

        mockMvc.perform(put("/api/products/{id}/images", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(imagesBody(urls)))
                .andExpect(status().isOk());
    }

    @Test
    void replaceProductImages_returns400_whenMoreThanTenImages() throws Exception {
        String[] urls = new String[11];
        for (int i = 0; i < urls.length; i++) {
            urls[i] = "https://cdn.example.com/" + i + ".png";
        }

        mockMvc.perform(put("/api/products/{id}/images", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(imagesBody(urls)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.images").exists());

        verify(productService, never()).replaceProductImages(anyLong(), any());
    }

    @Test
    void replaceProductImages_returns400_whenImagesIsMissing() throws Exception {
        mockMvc.perform(put("/api/products/{id}/images", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.images").exists());

        verify(productService, never()).replaceProductImages(anyLong(), any());
    }

    @Test
    void replaceProductImages_returns400_whenUrlIsBlank() throws Exception {
        mockMvc.perform(put("/api/products/{id}/images", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(imagesBody("   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['images[0].url']").exists());

        verify(productService, never()).replaceProductImages(anyLong(), any());
    }

    @Test
    void replaceProductImages_returns400_whenUrlIsMissing() throws Exception {
        mockMvc.perform(put("/api/products/{id}/images", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"images\":[{\"alt\":\"no url\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['images[0].url']").exists());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "javascript:alert(1)",
            "JAVASCRIPT:alert(1)",
            "ftp://cdn.example.com/a.png",
            "data:image/png;base64,AAAA",
            "file:///etc/passwd",
            "//cdn.example.com/a.png",
            "/relative/path.png",
            "https://",
            "https://exa mple.com/a.png",
            "not a url"
    })
    void replaceProductImages_returns400_forNonHttpUrls(String badUrl) throws Exception {
        mockMvc.perform(put("/api/products/{id}/images", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(imagesBody("https://cdn.example.com/ok.png", badUrl)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['images[1].url']").exists());

        verify(productService, never()).replaceProductImages(anyLong(), any());
    }

    @Test
    void replaceProductImages_returns400_whenUrlIsLongerThan500Characters() throws Exception {
        String longUrl = "https://cdn.example.com/" + "a".repeat(500);

        mockMvc.perform(put("/api/products/{id}/images", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(imagesBody(longUrl)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['images[0].url']").exists());
    }

    @Test
    void replaceProductImages_returns400_whenAltIsLongerThan255Characters() throws Exception {
        String body = objectMapper.writeValueAsString(new ProductImagesRequest(List.of(
                new ProductImagesRequest.Image("https://cdn.example.com/a.png", "x".repeat(256)))));

        mockMvc.perform(put("/api/products/{id}/images", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['images[0].alt']").exists());
    }

    @Test
    void replaceProductImages_returns404_whenProductMissing() throws Exception {
        when(productService.replaceProductImages(eq(99L), any(ProductImagesRequest.class)))
                .thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(put("/api/products/{id}/images", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(imagesBody("https://cdn.example.com/a.png")))
                .andExpect(status().isNotFound());
    }

    @Test
    void putIdAndPutIdImages_routeToDifferentHandlers() throws Exception {
        when(productService.updateProduct(anyLong(), any(ProductRequest.class))).thenReturn(response());
        when(productService.replaceProductImages(eq(1L), any(ProductImagesRequest.class))).thenReturn(detailResponse());

        mockMvc.perform(put("/api/products/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/products/{id}/images", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"images\":[]}"))
                .andExpect(status().isOk());

        verify(productService, times(1)).updateProduct(anyLong(), any(ProductRequest.class));
        verify(productService, times(1)).replaceProductImages(eq(1L), any(ProductImagesRequest.class));
    }

    // ------------------------------------------------------------------ ProductRequest: new optional fields

    private ProductRequest.ProductRequestBuilder validBuilder() {
        return ProductRequest.builder()
                .name("iPhone 15 Pro")
                .price(new BigDecimal("999.99"))
                .category("Electronics")
                .skuCode("APPLE-IP15P-128");
    }

    @Test
    void createProduct_returns201_andPassesAllNewOptionalFieldsToTheService() throws Exception {
        ProductRequest full = validBuilder()
                .brand("Apple")
                .mrp(new BigDecimal("1099.99"))
                .warranty("1 year limited warranty")
                .seller("Orbit Electronics Retail")
                .highlights(List.of("A17 Pro chip", "USB-C"))
                .specifications(List.of(
                        new ProductRequest.Specification("Display", "Screen Size", "6.1 inches"),
                        new ProductRequest.Specification("Display", "Refresh Rate", "120 Hz")))
                .build();
        when(productService.createProduct(any(ProductRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(full)))
                .andExpect(status().isCreated());

        ArgumentCaptor<ProductRequest> captor = ArgumentCaptor.forClass(ProductRequest.class);
        verify(productService).createProduct(captor.capture());
        assertThat(captor.getValue()).isEqualTo(full);
    }

    @Test
    void updateProduct_returns200_andDistinguishesAbsentFromEmptyLists() throws Exception {
        when(productService.updateProduct(anyLong(), any(ProductRequest.class))).thenReturn(response());

        mockMvc.perform(put("/api/products/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBuilder().build())))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/products/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                validBuilder().highlights(List.of()).specifications(List.of()).build())))
                .andExpect(status().isOk());

        ArgumentCaptor<ProductRequest> captor = ArgumentCaptor.forClass(ProductRequest.class);
        verify(productService, times(2)).updateProduct(anyLong(), captor.capture());
        assertThat(captor.getAllValues().get(0).getHighlights()).isNull();
        assertThat(captor.getAllValues().get(0).getSpecifications()).isNull();
        assertThat(captor.getAllValues().get(1).getHighlights()).isEmpty();
        assertThat(captor.getAllValues().get(1).getSpecifications()).isEmpty();
    }

    private void assertCreateRejected(ProductRequest invalid, String errorKey) throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['" + errorKey + "']").exists());
        verify(productService, never()).createProduct(any());
    }

    @Test
    void createProduct_returns400_whenBrandIsLongerThan100Characters() throws Exception {
        assertCreateRejected(validBuilder().brand("b".repeat(101)).build(), "brand");
    }

    @Test
    void createProduct_returns400_whenWarrantyIsLongerThan100Characters() throws Exception {
        assertCreateRejected(validBuilder().warranty("w".repeat(101)).build(), "warranty");
    }

    @Test
    void createProduct_returns400_whenSellerIsLongerThan100Characters() throws Exception {
        assertCreateRejected(validBuilder().seller("s".repeat(101)).build(), "seller");
    }

    @Test
    void createProduct_returns400_whenMrpIsZeroOrNegative() throws Exception {
        assertCreateRejected(validBuilder().mrp(new BigDecimal("0.00")).build(), "mrp");
        assertCreateRejected(validBuilder().mrp(new BigDecimal("-1.00")).build(), "mrp");
    }

    @Test
    void createProduct_returns400_whenMrpExceedsMaximum() throws Exception {
        assertCreateRejected(validBuilder().mrp(new BigDecimal("1000000.00")).build(), "mrp");
    }

    @Test
    void createProduct_returns400_whenMrpHasMoreThanTwoDecimals() throws Exception {
        assertCreateRejected(validBuilder().mrp(new BigDecimal("10.999")).build(), "mrp");
    }

    @Test
    void createProduct_returns400_whenMoreThanTenHighlights() throws Exception {
        List<String> eleven = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            eleven.add("highlight " + i);
        }
        assertCreateRejected(validBuilder().highlights(eleven).build(), "highlights");
    }

    @Test
    void createProduct_returns400_whenAHighlightIsBlank() throws Exception {
        assertCreateRejected(validBuilder().highlights(List.of("fine", "  ")).build(), "highlights[1]");
    }

    @Test
    void createProduct_returns400_whenAHighlightIsLongerThan200Characters() throws Exception {
        assertCreateRejected(validBuilder().highlights(List.of("h".repeat(201))).build(), "highlights[0]");
    }

    @Test
    void createProduct_returns400_whenMoreThanFiftySpecifications() throws Exception {
        List<ProductRequest.Specification> fiftyOne = new ArrayList<>();
        for (int i = 0; i < 51; i++) {
            fiftyOne.add(new ProductRequest.Specification("Group", "key " + i, "value"));
        }
        assertCreateRejected(validBuilder().specifications(fiftyOne).build(), "specifications");
    }

    @Test
    void createProduct_returns400_whenASpecificationGroupKeyOrValueIsBlank() throws Exception {
        assertCreateRejected(validBuilder().specifications(List.of(
                new ProductRequest.Specification(" ", "key", "value"))).build(), "specifications[0].group");
        assertCreateRejected(validBuilder().specifications(List.of(
                new ProductRequest.Specification("Group", "", "value"))).build(), "specifications[0].key");
        assertCreateRejected(validBuilder().specifications(List.of(
                new ProductRequest.Specification("Group", "key", null))).build(), "specifications[0].value");
    }

    @Test
    void createProduct_returns400_whenASpecificationFieldIsTooLong() throws Exception {
        assertCreateRejected(validBuilder().specifications(List.of(
                new ProductRequest.Specification("g".repeat(101), "key", "value"))).build(), "specifications[0].group");
        assertCreateRejected(validBuilder().specifications(List.of(
                new ProductRequest.Specification("Group", "k".repeat(101), "value"))).build(), "specifications[0].key");
        assertCreateRejected(validBuilder().specifications(List.of(
                new ProductRequest.Specification("Group", "key", "v".repeat(256)))).build(), "specifications[0].value");
    }

    @Test
    void updateProduct_returns400_whenNewFieldsAreInvalid() throws Exception {
        ProductRequest invalid = validBuilder()
                .mrp(new BigDecimal("-5.00"))
                .highlights(List.of(""))
                .build();

        mockMvc.perform(put("/api/products/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.mrp").exists())
                .andExpect(jsonPath("$.errors['highlights[0]']").exists());

        verify(productService, never()).updateProduct(anyLong(), any());
    }

    @Test
    void createAndUpdate_return400_whenServiceRejectsMrpLowerThanPrice() throws Exception {
        when(productService.createProduct(any(ProductRequest.class)))
                .thenThrow(new IllegalArgumentException("MRP cannot be lower than the price"));
        when(productService.updateProduct(anyLong(), any(ProductRequest.class)))
                .thenThrow(new IllegalArgumentException("MRP cannot be lower than the price"));
        String body = objectMapper.writeValueAsString(validBuilder().mrp(new BigDecimal("1.00")).build());

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("MRP cannot be lower than the price"));
        mockMvc.perform(put("/api/products/{id}", 1L).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("MRP cannot be lower than the price"));
    }
}
