package com.microservices.product.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * REST Client Configuration
 *
 * Configures the RestClient used to call inventory-service for live stock lookups on
 * single-item product reads
 *
 * @author Microservices Team
 * @version 1.0
 */
@Configuration
public class RestClientConfig {

    @Value("${product.inventory-service.url:http://localhost:8082}")
    private String inventoryServiceUrl;

    /**
     * Configure RestClient for calling inventory-service
     * Uses a 2000ms connect timeout and 3000ms read timeout
     *
     * @return RestClient configured with inventory-service's base URL and timeouts
     */
    @Bean
    public RestClient inventoryRestClient() {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(2000))
                .withReadTimeout(Duration.ofMillis(3000));

        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(settings);

        return RestClient.builder()
                .baseUrl(inventoryServiceUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
