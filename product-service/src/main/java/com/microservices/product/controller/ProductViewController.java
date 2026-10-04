package com.microservices.product.controller;

import com.microservices.product.client.InventoryClient;
import com.microservices.product.dto.response.ProductResponse;
import com.microservices.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * Product View Controller
 *
 * Serves server-rendered (Thymeleaf) HTML pages, as opposed to ProductController's JSON API.
 * The catalog page enriches one page of products with live stock using a single batch call
 * to inventory-service, rather than one HTTP call per row.
 *
 * @author Microservices Team
 * @version 1.0
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ProductViewController {

    private final ProductService productService;
    private final InventoryClient inventoryClient;

    /**
     * Show a page of the product catalog with live stock quantities
     *
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @param sortBy Sort field (default: id)
     * @param sortDir Sort direction (default: asc)
     * @param model View model
     * @return Name of the catalog view
     */
    @GetMapping("/catalog")
    public String showCatalog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            Model model) {
        log.debug("View request for product catalog - Page: {}, Size: {}, Sort: {} {}",
                  page, size, sortBy, sortDir);

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProductResponse> productPage = productService.getAllProducts(pageable);

        List<String> skuCodes = productPage.getContent().stream()
                .map(ProductResponse::getSkuCode)
                .toList();
        Map<String, Integer> stockBySku = inventoryClient.findStockForSkus(skuCodes);

        productPage.getContent().forEach(product ->
                product.setStockQuantity(stockBySku.get(product.getSkuCode())));

        model.addAttribute("page", productPage);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("stockUnavailable", !skuCodes.isEmpty() && stockBySku.isEmpty());

        return "catalog";
    }
}
