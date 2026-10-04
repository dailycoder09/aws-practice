package com.microservices.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Inventory Service Application
 *
 * Main entry point for the Inventory service
 *
 * @author Microservices Team
 * @version 1.0
 */
@SpringBootApplication
public class InventoryServiceApplication {

    /**
     * Main method to start the application
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
