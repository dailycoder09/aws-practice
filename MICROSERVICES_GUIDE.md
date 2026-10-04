# Spring Boot Microservices - Complete Guide & Reference

## 📋 Table of Contents
- [Project Overview](#project-overview)
- [Architecture Design](#architecture-design)
- [Technology Stack](#technology-stack)
- [Service Details](#service-details)
- [Communication Patterns](#communication-patterns)
- [Database Schemas](#database-schemas)
- [Configuration Management](#configuration-management)
- [Docker & Deployment](#docker--deployment)
- [API Documentation](#api-documentation)
- [Testing Guide](#testing-guide)
- [Interview Preparation](#interview-preparation)

---

## 🎯 Project Overview

**Use Case:** E-Commerce Order Management System

**Purpose:** Learn Spring Cloud Microservices with cloud-native patterns for interview preparation

**Key Learning Objectives:**
- Microservices architecture patterns
- Service discovery and registration
- API Gateway pattern
- Distributed configuration
- Inter-service communication (sync & async)
- Resilience patterns
- Observability and distributed tracing
- Containerization with Docker

---

## 🏗️ Architecture Design

### System Architecture Diagram

```
                                    ┌─────────────────┐
                                    │   API Gateway   │
                                    │  (Port: 8080)   │
                                    └────────┬────────┘
                                             │
                    ┌────────────────────────┼────────────────────────┐
                    │                        │                        │
            ┌───────▼───────┐       ┌───────▼────────┐      ┌───────▼────────┐
            │Product Service│       │ Order Service  │      │Inventory Service│
            │ (Port: 8081)  │       │ (Port: 8082)   │      │  (Port: 8083)   │
            └───────────────┘       └────────┬───────┘      └────────────────┘
                    │                        │                       │
                    │                        │                       │
            ┌───────▼───────┐       ┌────────▼────────┐     ┌───────▼────────┐
            │  PostgreSQL   │       │   PostgreSQL    │     │   PostgreSQL   │
            │ (products_db) │       │  (orders_db)    │     │(inventory_db)  │
            └───────────────┘       └─────────────────┘     └────────────────┘
                                             │
                                             │
                                    ┌────────▼─────────┐
                                    │   Kafka/RabbitMQ │
                                    │  Message Broker  │
                                    └────────┬─────────┘
                                             │
                                    ┌────────▼──────────┐
                                    │Notification Service│
                                    │   (Port: 8084)     │
                                    └───────────────────┘

                ┌─────────────────────────────────────────────────┐
                │        Infrastructure Services                  │
                ├─────────────────────────────────────────────────┤
                │  • Eureka Server (Port: 8761)                   │
                │  • Config Server (Port: 8888)                   │
                │  • Zipkin Server (Port: 9411)                   │
                └─────────────────────────────────────────────────┘
```

### Microservices Components

| Service | Port | Database | Purpose |
|---------|------|----------|---------|
| Eureka Server | 8761 | None | Service Discovery |
| Config Server | 8888 | None | Centralized Configuration |
| API Gateway | 8080 | None | Entry Point, Routing, Security |
| Product Service | 8081 | PostgreSQL | Product Catalog Management |
| Order Service | 8082 | PostgreSQL | Order Management |
| Inventory Service | 8083 | PostgreSQL | Stock Management |
| Notification Service | 8084 | None | Async Notifications |
| Zipkin | 9411 | None | Distributed Tracing |

---

## 🛠️ Technology Stack

### Core Technologies

```yaml
Framework:
  - Spring Boot: 3.2.1
  - Spring Cloud: 2023.0.0
  - Java: 17

Spring Cloud Components:
  - Spring Cloud Netflix Eureka: Service Discovery
  - Spring Cloud Gateway: API Gateway
  - Spring Cloud Config: Configuration Management
  - Spring Cloud OpenFeign: Declarative REST Client
  - Spring Cloud Sleuth: Distributed Tracing

Resilience:
  - Resilience4j: Circuit Breaker, Retry, Rate Limiter

Database:
  - PostgreSQL: 15.x
  - Spring Data JPA: ORM
  - Flyway/Liquibase: Database Migration

Message Broker:
  - Apache Kafka: 3.6.x (or RabbitMQ 3.12.x)
  - Spring Kafka: Event-driven communication

Observability:
  - Zipkin: Distributed Tracing
  - Spring Boot Actuator: Health Checks & Metrics
  - Micrometer: Metrics Collection

Containerization:
  - Docker: 24.x
  - Docker Compose: 2.x

Build Tool:
  - Maven: 3.9.x (or Gradle 8.x)

Testing:
  - JUnit 5: Unit Testing
  - Mockito: Mocking Framework
  - TestContainers: Integration Testing
  - RestAssured: API Testing
```

---

## 📦 Service Details

### 1. Eureka Server (Service Discovery)

**Purpose:** Central registry for all microservices

**Key Features:**
- Service registration
- Service discovery
- Health checks
- Load balancing metadata

**Dependencies:**
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```

**Application Properties:**
```yaml
server:
  port: 8761

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
  server:
    enable-self-preservation: false
```

---

### 2. Config Server (Centralized Configuration)

**Purpose:** Externalized configuration management

**Key Features:**
- Centralized configuration
- Environment-specific configs
- Dynamic refresh without restart
- Git-backed configuration

**Dependencies:**
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-config-server</artifactId>
</dependency>
```

**Application Properties:**
```yaml
server:
  port: 8888

spring:
  cloud:
    config:
      server:
        git:
          uri: file://${user.home}/config-repo
          default-label: main
```

---

### 3. API Gateway (Spring Cloud Gateway)

**Purpose:** Single entry point for all client requests

**Key Features:**
- Route requests to appropriate services
- Load balancing
- Authentication & Authorization
- Rate limiting
- Request/Response transformation

**Dependencies:**
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

**Routes Configuration:**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: product-service
          uri: lb://PRODUCT-SERVICE
          predicates:
            - Path=/api/products/**
          filters:
            - StripPrefix=1
        
        - id: order-service
          uri: lb://ORDER-SERVICE
          predicates:
            - Path=/api/orders/**
          filters:
            - StripPrefix=1
        
        - id: inventory-service
          uri: lb://INVENTORY-SERVICE
          predicates:
            - Path=/api/inventory/**
          filters:
            - StripPrefix=1
```

---

### 4. Product Service

**Purpose:** Manage product catalog

**Responsibilities:**
- CRUD operations for products
- Product search and filtering
- Category management

**Endpoints:**
```
GET    /api/products           - Get all products
GET    /api/products/{id}      - Get product by ID
POST   /api/products           - Create new product
PUT    /api/products/{id}      - Update product
DELETE /api/products/{id}      - Delete product
GET    /api/products/search    - Search products
```

**Database Schema:**
```sql
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    category VARCHAR(100),
    sku_code VARCHAR(50) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Dependencies:**
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
</dependencies>
```

---

### 5. Inventory Service

**Purpose:** Track product stock levels

**Responsibilities:**
- Check product availability
- Update stock levels
- Reserve inventory for orders
- Release reserved inventory

**Endpoints:**
```
GET    /api/inventory/{skuCode}           - Check stock availability
POST   /api/inventory/reserve              - Reserve inventory
POST   /api/inventory/release              - Release inventory
PUT    /api/inventory/{skuCode}/update     - Update stock level
```

**Database Schema:**
```sql
CREATE TABLE inventory (
    id BIGSERIAL PRIMARY KEY,
    sku_code VARCHAR(50) UNIQUE NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

### 6. Order Service

**Purpose:** Manage customer orders

**Responsibilities:**
- Create orders
- Validate product availability
- Communicate with Inventory Service
- Publish order events
- Order status management

**Endpoints:**
```
POST   /api/orders              - Create new order
GET    /api/orders/{id}         - Get order by ID
GET    /api/orders/user/{userId} - Get orders by user
PUT    /api/orders/{id}/status  - Update order status
```

**Database Schema:**
```sql
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT REFERENCES orders(id),
    sku_code VARCHAR(50) NOT NULL,
    product_name VARCHAR(255),
    price DECIMAL(10, 2) NOT NULL,
    quantity INTEGER NOT NULL
);
```

**Inter-service Communication:**
```java
@FeignClient(name = "INVENTORY-SERVICE")
public interface InventoryClient {
    @GetMapping("/api/inventory/{skuCode}")
    Boolean isInStock(@PathVariable String skuCode);
}

@FeignClient(name = "PRODUCT-SERVICE")
public interface ProductClient {
    @GetMapping("/api/products/{id}")
    ProductDTO getProduct(@PathVariable Long id);
}
```

---

### 7. Notification Service

**Purpose:** Send notifications asynchronously

**Responsibilities:**
- Listen to order events from Kafka
- Send email notifications
- Send SMS notifications (optional)
- Log notification history

**Kafka Consumer:**
```java
@KafkaListener(topics = "order-topic", groupId = "notification-group")
public void handleOrderCreated(OrderEvent event) {
    // Send notification
    sendEmailNotification(event);
}
```

---

## 🔄 Communication Patterns

### Synchronous Communication (REST + OpenFeign)

**Use Cases:**
- Order Service → Inventory Service (check stock)
- Order Service → Product Service (get product details)

**Example:**
```java
// In Order Service
@Autowired
private InventoryClient inventoryClient;

public OrderResponse createOrder(OrderRequest request) {
    // Check inventory
    boolean inStock = inventoryClient.isInStock(request.getSkuCode());
    
    if (!inStock) {
        throw new OutOfStockException("Product is out of stock");
    }
    
    // Create order
    // ...
}
```

### Asynchronous Communication (Kafka)

**Use Cases:**
- Order Service → Notification Service (order created event)

**Producer (Order Service):**
```java
@Autowired
private KafkaTemplate<String, OrderEvent> kafkaTemplate;

public void publishOrderEvent(Order order) {
    OrderEvent event = new OrderEvent(
        order.getId(),
        order.getOrderNumber(),
        order.getUserId(),
        order.getTotalAmount()
    );
    
    kafkaTemplate.send("order-topic", event);
}
```

**Consumer (Notification Service):**
```java
@KafkaListener(topics = "order-topic", groupId = "notification-group")
public void handleOrderCreated(OrderEvent event) {
    log.info("Received order event: {}", event);
    sendNotification(event);
}
```

---

## 🗄️ Database Schemas

### Product Service Database (products_db)

```sql
-- Products Table
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    category VARCHAR(100),
    sku_code VARCHAR(50) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sku_code ON products(sku_code);
CREATE INDEX idx_category ON products(category);
```

### Inventory Service Database (inventory_db)

```sql
-- Inventory Table
CREATE TABLE inventory (
    id BIGSERIAL PRIMARY KEY,
    sku_code VARCHAR(50) UNIQUE NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_quantity CHECK (quantity >= 0),
    CONSTRAINT chk_reserved CHECK (reserved_quantity >= 0)
);

CREATE INDEX idx_inventory_sku ON inventory(sku_code);
```

### Order Service Database (orders_db)

```sql
-- Orders Table
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Order Items Table
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT REFERENCES orders(id) ON DELETE CASCADE,
    sku_code VARCHAR(50) NOT NULL,
    product_name VARCHAR(255),
    price DECIMAL(10, 2) NOT NULL,
    quantity
