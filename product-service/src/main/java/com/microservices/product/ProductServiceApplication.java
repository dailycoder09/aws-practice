package com.microservices.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Product Service Application
 *
 * Main entry point for the Product service
 *
 * @author Microservices Team
 * @version 1.0
 */
@SpringBootApplication
public class ProductServiceApplication {

    /**
     * Main method to start the application
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
