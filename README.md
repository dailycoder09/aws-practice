# 🚀 Spring Boot Microservices - Complete Reference Guide

> **E-Commerce Order Management System**  
> Complete End-to-End Guide for Interview Preparation with Cloud-Native Patterns

---

## 📋 Table of Contents

1. [Project Overview](#-project-overview)
2. [Architecture Design](#-architecture-design)
3. [Technology Stack](#-technology-stack)
4. [Project Structure](#-project-structure)
5. [Database Schemas](#-database-schemas)
6. [API Documentation](#-api-documentation)
7. [Configuration Files](#-configuration-files)
8. [Resilience Patterns](#-resilience-patterns)
9. [Docker Setup](#-docker-setup)
10. [Setup & Installation](#-setup--installation)
11. [Testing Strategy](#-testing-strategy)
12. [Interview Preparation](#-interview-preparation)

---

## 🎯 Project Overview

### Use Case
**E-Commerce Order Management System** - A production-grade microservices architecture demonstrating industry best practices and cloud-native patterns.

### Business Flow
```
User → Browse Products → Select Product → Create Order 
  → Check Inventory → Validate Product → Reserve Stock 
  → Publish Event → Send Notification
```

### Learning Objectives
✅ Microservices Architecture Patterns  
✅ Service Discovery & Registration (Eureka)  
✅ API Gateway Pattern (Spring Cloud Gateway)  
✅ Centralized Configuration (Config Server)  
✅ Inter-service Communication (REST + Kafka)  
✅ Resilience Patterns (Circuit Breaker, Retry, Rate Limiting)  
✅ Distributed Tracing (Zipkin)  
✅ Database per Service Pattern  
✅ Event-Driven Architecture  
✅ Containerization (Docker & Docker Compose)  

---

## 🏗️ Architecture Design

### System Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                      CLIENT LAYER                            │
│            (Browser, Mobile App, Postman)                    │
└────────────────────────┬─────────────────────────────────────┘
                         │ HTTPS
              ┌──────────▼──────────┐
              │   API GATEWAY       │ ← Single Entry Point
              │   Port: 8080        │   • Routing
              │                     │   • Load Balancing
              │ Spring Cloud        │   • Authentication
              │ Gateway             │   • Rate Limiting
              └──────────┬──────────┘
                         │
    ┌────────────────────┼────────────────────┐
    │                    │                    │
┌───▼──────┐     ┌───────▼───────┐     ┌─────▼──────┐
│ Product  │     │    Order      │     │ Inventory  │
│ Service  │◄────│   Service     │────►│  Service   │
│Port 8081 │Feign│  Port 8082    │Feign│ Port 8083  │
│          │     │               │     │            │
│PostgreSQL│     │  PostgreSQL   │     │ PostgreSQL │
│products  │     │   orders      │     │ inventory  │
└──────────┘     └───────┬───────┘     └────────────┘
                         │
                         │ Kafka Event
                         │
                ┌────────▼────────┐
                │     Kafka       │
                │ Message Broker  │
                │  Port: 9092     │
                └────────┬────────┘
                         │
                ┌────────▼─────────┐
                │  Notification    │
                │    Service       │
                │  Port: 8084      │
                │                  │
                │ (Email/SMS)      │
                └──────────────────┘

┌──────────────────────────────────────────────────────────────┐
│              INFRASTRUCTURE SERVICES                         │
├──────────────────────────────────────────────────────────────┤
│ • Eureka Server (8761)  - Service Discovery                 │
│ • Config Server (8888)  - Centralized Configuration         │
│ • Zipkin (9411)        - Distributed Tracing                │
└──────────────────────────────────────────────────────────────┘
```

### Services Overview

| Service | Port | Database | Purpose |
|---------|------|----------|---------|
| **Eureka Server** | 8761 | - | Service Discovery & Registration |
| **Config Server** | 8888 | Git Repo | Centralized Configuration |
| **API Gateway** | 8080 | - | Entry Point, Routing, Security |
| **Product Service** | 8081 | PostgreSQL | Product Catalog Management |
| **Order Service** | 8082 | PostgreSQL | Order Management & Processing |
| **Inventory Service** | 8083 | PostgreSQL | Stock Management |
| **Notification Service** | 8084 | - | Async Notifications (Email/SMS) |
| **Zipkin** | 9411 | In-Memory | Distributed Tracing |

---

## 🛠️ Technology Stack

### Core Technologies

```yaml
Framework:
  Spring Boot: 3.2.1
  Spring Cloud: 2023.0.0
  Java: 17 (LTS)

Spring Cloud Components:
  - spring-cloud-starter-netflix-eureka-server
  - spring-cloud-starter-netflix-eureka-client
  - spring-cloud-starter-gateway
  - spring-cloud-config-server
  - spring-cloud-starter-config
  - spring-cloud-starter-openfeign

Resilience:
  - resilience4j-spring-boot3
  - resilience4j-circuitbreaker
  - resilience4j-retry
  - resilience4j-ratelimiter

Database:
  - PostgreSQL 15.x
  - spring-boot-starter-data-jpa
  - flyway-core (Database Migration)

Messaging:
  - Apache Kafka 3.6.x
  - spring-kafka

Observability:
  - Zipkin Server
  - spring-boot-starter-actuator
  - micrometer-tracing-bridge-brave

Containerization:
  - Docker 24.x
  - Docker Compose 2.x

Build Tool:
  - Maven 3.9.x

Testing:
  - JUnit 5
  - Mockito
  - TestContainers
  - Rest-Assured

Utilities:
  - Lombok 1.18.30
  - MapStruct 1.5.5
```

### Maven Parent POM

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.1</version>
</parent>

<properties>
    <java.version>17</java.version>
    <spring-cloud.version>2023.0.0</spring-cloud.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---

## 📁 Project Structure

```
microservices-demo/
├── eureka-server/
│   ├── src/main/java/com/microservices/eureka/
│   │   └── EurekaServerApplication.java
│   ├── src/main/resources/
│   │   └── application.yml
│   └── pom.xml
│
├── config-server/
│   ├── src/main/java/com/microservices/config/
│   │   └── ConfigServerApplication.java
│   ├── src/main/resources/
│   │   └── application.yml
│   └── pom.xml
│
├── api-gateway/
│   ├── src/main/java/com/microservices/gateway/
│   │   ├── ApiGatewayApplication.java
│   │   ├── filter/
│   │   │   └── LoggingFilter.java
│   │   └── controller/
│   │       └── FallbackController.java
│   ├── src/main/resources/
│   │   └── application.yml
│   └── pom.xml
│
├── product-service/
│   ├── src/main/java/com/microservices/product/
│   │   ├── ProductServiceApplication.java
│   │   ├── controller/
│   │   │   └── ProductController.java
│   │   ├── service/
│   │   │   ├── ProductService.java
│   │   │   └── ProductServiceImpl.java
│   │   ├── repository/
│   │   │   └── ProductRepository.java
│   │   ├── model/
│   │   │   └── Product.java
│   │   ├── dto/
│   │   │   ├── ProductRequest.java
│   │   │   └── ProductResponse.java
│   │   └── exception/
│   │       ├── ProductNotFoundException.java
│   │       └── GlobalExceptionHandler.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/
│   │       └── V1__create_products_table.sql
│   └── pom.xml
│
├── order-service/
│   ├── src/main/java/com/microservices/order/
│   │   ├── OrderServiceApplication.java
│   │   ├── controller/
│   │   │   └── OrderController.java
│   │   ├── service/
│   │   │   └── OrderService.java
│   │   ├── repository/
│   │   │   ├── OrderRepository.java
│   │   │   └── OrderItemRepository.java
│   │   ├── model/
│   │   │   ├── Order.java
│   │   │   └── OrderItem.java
│   │   ├── dto/
│   │   │   ├── OrderRequest.java
│   │   │   └── OrderResponse.java
│   │   ├── client/
│   │   │   ├── InventoryClient.java
│   │   │   └── ProductClient.java
│   │   ├── event/
│   │   │   ├── OrderEvent.java
│   │   │   └── OrderEventPublisher.java
│   │   └── config/
│   │       ├── FeignConfig.java
│   │       └── KafkaProducerConfig.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/
│   │       └── V1__create_orders_tables.sql
│   └── pom.xml
│
├── inventory-service/
│   ├── src/main/java/com/microservices/inventory/
│   │   ├── InventoryServiceApplication.java
│   │   ├── controller/
│   │   │   └── InventoryController.java
│   │   ├── service/
│   │   │   └── InventoryService.java
│   │   ├── repository/
│   │   │   └── InventoryRepository.java
│   │   ├── model/
│   │   │   └── Inventory.java
│   │   └── dto/
│   │       ├── InventoryRequest.java
│   │       └── InventoryResponse.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/
│   │       └── V1__create_inventory_table.sql
│   └── pom.xml
│
├── notification-service/
│   ├── src/main/java/com/microservices/notification/
│   │   ├── NotificationServiceApplication.java
│   │   ├── listener/
│   │   │   └── OrderEventListener.java
│   │   ├── service/
│   │   │   ├── NotificationService.java
│   │   │   └── EmailService.java
│   │   ├── event/
│   │   │   └── OrderEvent.java
│   │   └── config/
│   │       └── KafkaConsumerConfig.java
│   ├── src/main/resources/
│   │   └── application.yml
│   └── pom.xml
│
├── config-repo/ (Git repository for configurations)
│   ├── product-service.yml
│   ├── order-service.yml
│   ├── inventory-service.yml
│   ├── notification-service.yml
│   ├── api-gateway.yml
│   └── application.yml
│
├── docker-compose.yml
├── pom.xml (Parent POM)
└── README.md
```

---

## 🗄️ Database Schemas

### Product Service Database

```sql
-- products_db
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

### Inventory Service Database

```sql
-- inventory_db
CREATE TABLE inventory (
    id BIGSERIAL PRIMARY KEY,
    sku_code VARCHAR(50) UNIQUE NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_quantity CHECK (quantity >= 0),
    CONSTRAINT chk_reserved CHECK (reserved_quantity >= 0),
    CONSTRAINT chk_available CHECK (quantity >= reserved_quantity)
);

CREATE INDEX idx_sku_code ON inventory(sku_code);
```

### Order Service Database

```sql
-- orders_db
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
    order_id BIGINT REFERENCES orders(id) ON DELETE CASCADE,
    sku_code VARCHAR(50) NOT NULL,
    product_name VARCHAR(255),
    price DECIMAL(10, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    CONSTRAINT chk_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_order_number ON orders(order_number);
CREATE INDEX idx_user_id ON orders(user_id);
CREATE INDEX idx_order_id ON order_items(order_id);
```

---

## 📡 API Documentation

### Product Service APIs

```
Base URL: http://localhost:8080/api/products (via API Gateway)
Direct URL: http://localhost:8081/api/products

GET    /api/products              - Get all products
GET    /api/products/{id}         - Get product by ID
GET    /api/products/sku/{skuCode} - Get product by SKU
POST   /api/products              - Create new product
PUT    /api/products/{id}         - Update product
DELETE /api/products/{id}         - Delete product
GET    /api
