package com.microservices.inventory.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI Configuration
 *
 * Configures Swagger/OpenAPI documentation for the Inventory Service
 * Provides comprehensive API documentation accessible via /swagger-ui.html
 *
 * @author Microservices Team
 * @version 1.0
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:inventory-service}")
    private String applicationName;

    @Value("${server.port:8082}")
    private String serverPort;

    /**
     * Configure OpenAPI documentation
     *
     * @return OpenAPI configuration
     */
    @Bean
    public OpenAPI inventoryServiceOpenAPI() {
        Server localServer = new Server();
        localServer.setUrl("http://localhost:" + serverPort);
        localServer.setDescription("Local Development Server");

        Contact contact = new Contact();
        contact.setName("Microservices Team");
        contact.setEmail("support@microservices.com");
        contact.setUrl("https://microservices.com");

        License license = new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");

        Info info = new Info()
                .title("Inventory Service API")
                .version("1.0.0")
                .description("RESTful API for Inventory Management in E-Commerce Microservices Architecture")
                .contact(contact)
                .license(license)
                .termsOfService("https://microservices.com/terms");

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
}
