package com.microservices.product.controller;

import com.microservices.product.client.InventoryClient;
import com.microservices.product.dto.response.ProductResponse;
import com.microservices.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ProductViewController.class)
class ProductViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private InventoryClient inventoryClient;

    private ProductResponse response(String skuCode) {
        return ProductResponse.builder()
                .id(1L)
                .name("iPhone 15 Pro")
                .price(new BigDecimal("999.99"))
                .category("Electronics")
                .skuCode(skuCode)
                .build();
    }

    @Test
    void showCatalog_returns200_withCatalogViewAndProducts() throws Exception {
        Page<ProductResponse> page = new PageImpl<>(List.of(response("APPLE-IP15P-128")), PageRequest.of(0, 20), 1);
        when(productService.getAllProducts(any())).thenReturn(page);
        when(inventoryClient.findStockForSkus(any())).thenReturn(Map.of("APPLE-IP15P-128", 42));

        mockMvc.perform(get("/catalog"))
                .andExpect(status().isOk())
                .andExpect(view().name("catalog"))
                .andExpect(model().attributeExists("products"))
                .andExpect(model().attributeExists("page"))
                .andExpect(model().attribute("sortBy", "id"))
                .andExpect(model().attribute("sortDir", "asc"));
    }

    @Test
    void showCatalog_mergesStockQuantity_fromBatchInventoryLookup() throws Exception {
        Page<ProductResponse> page = new PageImpl<>(List.of(response("APPLE-IP15P-128")), PageRequest.of(0, 20), 1);
        when(productService.getAllProducts(any())).thenReturn(page);
        when(inventoryClient.findStockForSkus(any())).thenReturn(Map.of("APPLE-IP15P-128", 42));

        MvcResult result = mockMvc.perform(get("/catalog"))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        List<ProductResponse> products = (List<ProductResponse>) result.getModelAndView().getModel().get("products");

        assertThat(products).hasSize(1);
        assertThat(products.get(0).getStockQuantity()).isEqualTo(42);
    }

    @Test
    void showCatalog_leavesStockQuantityNull_whenSkuMissingFromInventoryResponse() throws Exception {
        Page<ProductResponse> page = new PageImpl<>(List.of(response("UNKNOWN-SKU")), PageRequest.of(0, 20), 1);
        when(productService.getAllProducts(any())).thenReturn(page);
        when(inventoryClient.findStockForSkus(any())).thenReturn(Map.of());

        MvcResult result = mockMvc.perform(get("/catalog"))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        List<ProductResponse> products = (List<ProductResponse>) result.getModelAndView().getModel().get("products");

        assertThat(products).hasSize(1);
        assertThat(products.get(0).getStockQuantity()).isNull();
    }

    @Test
    void showCatalog_makesSingleBatchCall_withSkuCodesFromPage() throws Exception {
        Page<ProductResponse> page = new PageImpl<>(
                List.of(response("APPLE-IP15P-128"), response("SAMSUNG-S24-256")), PageRequest.of(0, 20), 2);
        when(productService.getAllProducts(any())).thenReturn(page);
        when(inventoryClient.findStockForSkus(any())).thenReturn(Map.of());

        mockMvc.perform(get("/catalog")).andExpect(status().isOk());

        verify(inventoryClient, times(1)).findStockForSkus(List.of("APPLE-IP15P-128", "SAMSUNG-S24-256"));
        verifyNoMoreInteractions(inventoryClient);
    }

    @Test
    void showCatalog_rendersStockNumberOrEmDash_perRow() throws Exception {
        Page<ProductResponse> page = new PageImpl<>(
                List.of(response("APPLE-IP15P-128"), response("NO-STOCK-SKU")), PageRequest.of(0, 20), 2);
        when(productService.getAllProducts(any())).thenReturn(page);
        when(inventoryClient.findStockForSkus(any())).thenReturn(Map.of("APPLE-IP15P-128", 42));

        mockMvc.perform(get("/catalog"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">APPLE-IP15P-128</td>")))
                .andExpect(content().string(containsString(">42</span>")))
                .andExpect(content().string(containsString(">—</span>")));
    }

    @Test
    void showCatalog_passesPagingAndSortToService_andPaginationLinksPreserveThem() throws Exception {
        Pageable requested = PageRequest.of(1, 1, Sort.by("name").descending());
        Page<ProductResponse> page = new PageImpl<>(List.of(response("APPLE-IP15P-128")), requested, 3);
        when(productService.getAllProducts(any())).thenReturn(page);
        when(inventoryClient.findStockForSkus(any())).thenReturn(Map.of());

        mockMvc.perform(get("/catalog")
                        .param("page", "1")
                        .param("size", "1")
                        .param("sortBy", "name")
                        .param("sortDir", "desc"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Page 2 of 3")))
                .andExpect(content().string(containsString("/catalog?page=0&amp;size=1&amp;sortBy=name&amp;sortDir=desc")))
                .andExpect(content().string(containsString("/catalog?page=2&amp;size=1&amp;sortBy=name&amp;sortDir=desc")));

        verify(productService).getAllProducts(requested);
    }

    @Test
    void showCatalog_disablesPreviousOnFirstPage_andNextOnLastPage() throws Exception {
        Page<ProductResponse> page = new PageImpl<>(List.of(response("APPLE-IP15P-128")), PageRequest.of(0, 20), 1);
        when(productService.getAllProducts(any())).thenReturn(page);
        when(inventoryClient.findStockForSkus(any())).thenReturn(Map.of());

        mockMvc.perform(get("/catalog"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Page 1 of 1")))
                .andExpect(content().string(not(containsString("href=\"/catalog?page="))));
    }

    @Test
    void showCatalog_marksStockLevelsWithBadgeClasses() throws Exception {
        Page<ProductResponse> page = new PageImpl<>(
                List.of(response("PLENTY"), response("FEW"), response("NONE"), response("UNKNOWN")),
                PageRequest.of(0, 20), 4);
        when(productService.getAllProducts(any())).thenReturn(page);
        when(inventoryClient.findStockForSkus(any())).thenReturn(Map.of("PLENTY", 42, "FEW", 7, "NONE", 0));

        mockMvc.perform(get("/catalog"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("stock stock--ok")))
                .andExpect(content().string(containsString("stock stock--low")))
                .andExpect(content().string(containsString("stock stock--out")))
                .andExpect(content().string(containsString("stock stock--unknown")));
    }

    @Test
    void showCatalog_warnsThatStockIsUnavailable_whenNoStockCameBackForANonEmptyPage() throws Exception {
        Page<ProductResponse> page = new PageImpl<>(List.of(response("APPLE-IP15P-128")), PageRequest.of(0, 20), 1);
        when(productService.getAllProducts(any())).thenReturn(page);
        when(inventoryClient.findStockForSkus(any())).thenReturn(Map.of());

        mockMvc.perform(get("/catalog"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("stockUnavailable", true))
                .andExpect(content().string(containsString("No stock data came back for this page")));
    }

    @Test
    void showCatalog_doesNotWarn_whenStockCameBack() throws Exception {
        Page<ProductResponse> page = new PageImpl<>(List.of(response("APPLE-IP15P-128")), PageRequest.of(0, 20), 1);
        when(productService.getAllProducts(any())).thenReturn(page);
        when(inventoryClient.findStockForSkus(any())).thenReturn(Map.of("APPLE-IP15P-128", 42));

        mockMvc.perform(get("/catalog"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("stockUnavailable", false))
                .andExpect(content().string(not(containsString("No stock data came back"))));
    }

    @Test
    void showCatalog_showsEmptyMessage_whenThereAreNoProducts() throws Exception {
        Page<ProductResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(productService.getAllProducts(any())).thenReturn(page);
        when(inventoryClient.findStockForSkus(any())).thenReturn(Map.of());

        mockMvc.perform(get("/catalog"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("stockUnavailable", false))
                .andExpect(content().string(containsString("No products found.")));
    }
}
