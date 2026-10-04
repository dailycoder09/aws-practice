package com.microservices.inventory.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * RestClient Configuration
 *
 * Configures the Spring 6.1 {@link RestClient} used by {@code ProductClient}
 * to call product-service. A short connect/read timeout keeps a slow or
 * unreachable product-service from blocking inventory-service's write path
 * indefinitely.
 *
 * @author Microservices Team
 * @version 1.0
 */
@Configuration
public class RestClientConfig {

    private static final int CONNECT_TIMEOUT_MS = 2000;
    private static final int READ_TIMEOUT_MS = 3000;

    @Value("${inventory.product-service.url}")
    private String productServiceUrl;

    /**
     * Configure the RestClient used to call product-service
     *
     * @return RestClient pointed at product-service's base URL
     */
    @Bean
    public RestClient productServiceRestClient() {
        return RestClient.builder()
                .baseUrl(productServiceUrl)
                .requestFactory(productServiceClientHttpRequestFactory())
                .build();
    }

    private ClientHttpRequestFactory productServiceClientHttpRequestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        requestFactory.setReadTimeout(READ_TIMEOUT_MS);
        return requestFactory;
    }
}
