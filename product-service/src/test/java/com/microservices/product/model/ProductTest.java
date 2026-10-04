package com.microservices.product.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTest {

    @Test
    void validateEntity_trimsNameCategoryAndUppercasesSku() {
        Product product = Product.builder()
                .name("  iPhone 15 Pro  ")
                .price(new BigDecimal("999.99"))
                .category("  Electronics  ")
                .skuCode("  apple-ip15p-128  ")
                .build();

        product.validateEntity();

        assertThat(product.getName()).isEqualTo("iPhone 15 Pro");
        assertThat(product.getCategory()).isEqualTo("Electronics");
        assertThat(product.getSkuCode()).isEqualTo("APPLE-IP15P-128");
    }

    @Test
    void validateEntity_toleratesNullFields() {
        Product product = Product.builder()
                .price(new BigDecimal("10.00"))
                .build();

        product.validateEntity();

        assertThat(product.getName()).isNull();
        assertThat(product.getCategory()).isNull();
        assertThat(product.getSkuCode()).isNull();
    }
}
