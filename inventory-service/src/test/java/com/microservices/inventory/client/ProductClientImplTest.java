package com.microservices.inventory.client;

import com.microservices.inventory.dto.response.ProductSummary;
import com.microservices.inventory.exception.ProductServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.NOT_FOUND;

class ProductClientImplTest {

    private static final String BASE_URL = "http://product-service.test";

    private MockRestServiceServer mockServer;
    private ProductClientImpl productClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        this.mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        this.productClient = new ProductClientImpl(restClient);
    }

    @Test
    void findBySkuCode_returnsProductSummary_whenProductServiceRespondsWith200() {
        mockServer.expect(requestTo(BASE_URL + "/api/products/sku/APPLE-IP15P-128"))
                .andExpect(method(GET))
                .andRespond(withSuccess(
                        "{\"skuCode\":\"APPLE-IP15P-128\",\"name\":\"iPhone 15 Pro\",\"price\":999.99,\"category\":\"Electronics\"}",
                        MediaType.APPLICATION_JSON));

        Optional<ProductSummary> result = productClient.findBySkuCode("APPLE-IP15P-128");

        assertThat(result).isPresent();
        assertThat(result.get().getSkuCode()).isEqualTo("APPLE-IP15P-128");
        assertThat(result.get().getName()).isEqualTo("iPhone 15 Pro");
        mockServer.verify();
    }

    @Test
    void findBySkuCode_returnsEmpty_whenProductServiceRespondsWith404() {
        mockServer.expect(requestTo(BASE_URL + "/api/products/sku/UNKNOWN-SKU"))
                .andExpect(method(GET))
                .andRespond(withStatus(NOT_FOUND));

        Optional<ProductSummary> result = productClient.findBySkuCode("UNKNOWN-SKU");

        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    void findBySkuCode_throwsProductServiceUnavailableException_whenProductServiceRespondsWith500() {
        mockServer.expect(requestTo(BASE_URL + "/api/products/sku/APPLE-IP15P-128"))
                .andExpect(method(GET))
                .andRespond(withServerError());

        assertThatThrownBy(() -> productClient.findBySkuCode("APPLE-IP15P-128"))
                .isInstanceOf(ProductServiceUnavailableException.class);
        mockServer.verify();
    }

    @Test
    void findBySkuCode_throwsProductServiceUnavailableException_whenConnectionFails() {
        mockServer.expect(requestTo(BASE_URL + "/api/products/sku/APPLE-IP15P-128"))
                .andExpect(method(GET))
                .andRespond(request -> {
                    throw new IOException("Simulated connection refused");
                });

        assertThatThrownBy(() -> productClient.findBySkuCode("APPLE-IP15P-128"))
                .isInstanceOf(ProductServiceUnavailableException.class);
        mockServer.verify();
    }
}
