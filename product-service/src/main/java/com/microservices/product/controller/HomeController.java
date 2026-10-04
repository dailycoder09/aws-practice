package com.microservices.product.controller;

import com.microservices.product.service.ProductService;
import com.microservices.product.service.ServerInfoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Home Controller
 *
 * Serves the landing page: which server answered this request, plus a catalog summary.
 *
 * @author Microservices Team
 * @version 1.0
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ServerInfoService serverInfoService;
    private final ProductService productService;

    @GetMapping("/")
    public String home(HttpServletRequest request, Model model) {
        model.addAttribute("server", serverInfoService.describe(request));
        model.addAttribute("productCount", productService.getAllProducts(PageRequest.of(0, 1)).getTotalElements());
        model.addAttribute("categoryCount", productService.getAllCategories().size());
        return "index";
    }
}
