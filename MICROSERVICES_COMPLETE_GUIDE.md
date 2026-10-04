# 🚀 Spring Boot Microservices - Complete End-to-End Guide

## 📋 Table of Contents
1. [Project Overview](#project-overview)
2. [Architecture Design](#architecture-design)
3. [Technology Stack](#technology-stack)
4. [Service Details](#service-details)
5. [Communication Patterns](#communication-patterns)
6. [Database Schemas](#database-schemas)
7. [Configuration Management](#configuration-management)
8. [Resilience Patterns](#resilience-patterns)
9. [Docker & Deployment](#docker--deployment)
10. [API Documentation](#api-documentation)
11. [Setup & Installation](#setup--installation)
12. [Testing Guide](#testing-guide)
13. [Interview Preparation](#interview-preparation)

---

## 🎯 Project Overview

### Use Case: E-Commerce Order Management System

**Purpose:** Comprehensive Spring Cloud Microservices project for interview preparation with cloud-native patterns.

### Key Learning Objectives
✅ Microservices architecture patterns  
✅ Service discovery and registration  
✅ API Gateway pattern  
✅ Distributed configuration  
✅ Inter-service communication (sync & async)  
✅ Resilience patterns (Circuit Breaker, Retry, Rate Limiting)  
✅ Observability and distributed tracing  
✅ Containerization with Docker  
✅ Event-driven architecture  
✅ Database per service pattern  

### Business Flow
```
1. User browses products (Product Service)
2. User creates order (Order Service)
3. Order Service checks inventory (Inventory Service)
4. Order Service validates product details (Product Service)
5. Order created successfully
6. Event published to Kafka (Order Created Event)
7. Notification Service sends confirmation (Email/SMS)
```

---

## 🏗️ Architecture Design

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                            CLIENT LAYER                                  │
│                    (Browser, Mobile App, Postman)                        │
└───────────────────────────────┬─────────────────────────────────────────┘
                                │
                                │ HTTP Requests
                                │
                    ┌───────────▼──────────┐
                    │   API GATEWAY        │
                    │   (Port: 8080)       │
                    │ - Routing            │
                    │ - Load Balancing     │
                    │ - Authentication     │
                    │ - Rate Limiting      │
                    └───────────┬──────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        │                       │                       │
┌───────▼────────┐    ┌────────▼────────┐    ┌────────▼────────┐
│ Product Service│    │ Order Service   │    │Inventory Service│
│  (Port: 8081)  │    │  (Port: 8082)   │    │  (Port: 8083)   │
│                │    │                 │    │                 │
│ - CRUD Ops     │◄───┤ - Create Orders │───►│ - Check Stock   │
│ - Search       │    │ - Feign Client  │    │ - Reserve Stock │
└───────┬────────┘    └────────┬────────┘    └────────┬────────┘
        │                      │                       │
        │                      │                       │
┌───────▼────────┐    ┌────────▼────────┐    ┌────────▼────────┐
│  PostgreSQL    │    │  PostgreSQL     │    │  PostgreSQL     │
│ products_db    │    │  orders_db      │    │ inventory_db    │
└────────────────┘    └────────┬────────┘    └─────────────────┘
                               │
                               │ Kafka Message
                               │
                      ┌────────▼─────────┐
                      │  Apache Kafka    │
                      │  Message Broker  │
                      │  (Port: 9092)    │
                      └────────┬─────────┘
                               │
                      ┌────────▼──────────┐
                      │ Notification      │
                      │ Service           │
                      │ (Port: 8084)      │
                      │ - Email           │
                      │ - SMS (Optional)  │
                      └───────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                    INFRASTRUCTURE SERVICES                               │
├─────────────────────────────────────────────────────────────────────────┤
│  • Eureka Server (8761)    - Service Discovery & Registration           │
│  • Config Server (8888)    - Centralized Configuration                  │
│  • Zipkin (9411)          - Distributed Tracing & Monitoring            │
└─────────────────────────────────────────────────────────────────────────┘
```

### Microservices Components Table

| Service | Port | Database | Technology | Purpose |
|---------|------|----------|------------|---------|
| Eureka Server | 8761 | None | Spring Cloud Netflix | Service Discovery |
| Config Server | 8888 | Git Repo | Spring Cloud Config | Centralized Configuration |
| API Gateway | 8080 | None | Spring Cloud Gateway | Entry Point, Routing |
| Product Service | 8081 | PostgreSQL | Spring Boot, JPA | Product Catalog |
| Order Service | 8082 | PostgreSQL | Spring Boot, JPA, Kafka | Order Management |
| Inventory Service | 8083 | PostgreSQL | Spring Boot, JPA | Stock Management |
| Notification Service | 8084 | None | Spring Boot, Kafka | Async Notifications |
| Zipkin | 9411 | In-Memory | Zipkin Server | Distributed Tracing |
| Kafka | 9092 | Disk | Apache Kafka | Message Broker |
| PostgreSQL | 5432 | Disk | PostgreSQL 15 | Relational Database |

---

## 🛠️ Technology Stack

### Complete Technology Matrix

```yaml
Core Framework:
  Spring Boot: 3.2.1
  Spring Cloud: 2023.0.0
  Java Version: 17 (LTS)
  
Spring Cloud Components:
  spring-cloud-starter-netflix-eureka-server: Service Discovery
  spring-cloud-starter-netflix-eureka-client: Service Registration
  spring-cloud-starter-gateway: API Gateway
  spring-cloud-config-server: Config Server
  spring-cloud-starter-config: Config Client
  spring-cloud-starter-openfeign: Declarative REST Client
  spring-cloud-sleuth: Distributed Tracing
  
Resilience & Fault Tolerance:
  resilience4j-spring-boot3: Circuit Breaker
  resilience4j-circuitbreaker: Circuit Breaker Pattern
  resilience4j-retry: Retry Pattern
  resilience4j-ratelimiter: Rate Limiting
  resilience4j-timelimiter: Timeout
  
Database & Persistence:
  PostgreSQL: 15.x
  spring-boot-starter-data-jpa: ORM Framework
  hibernate-core: JPA Implementation
  flyway-core: Database Migration
  
Messaging:
  Apache Kafka: 3.6.x
  spring-kafka: Spring Integration
  kafka-clients: Kafka Client Library
  
Observability & Monitoring:
  zipkin-server: Distributed Tracing
  spring-boot-starter-actuator: Health Checks & Metrics
  micrometer-registry-prometheus: Metrics Collection
  
Containerization:
  Docker: 24.x
  Docker Compose: 2.x
  
Build & Dependency Management:
  Maven: 3.9.x
  maven-compiler-plugin: 3.11.0
  
Testing:
  junit-jupiter: 5.10.x (Unit Testing)
  mockito-core: 5.x (Mocking)
  testcontainers: 1.19.x (Integration Testing)
  rest-assured: 5.x (API Testing)
  spring-boot-starter-test: Testing Utilities
  
Documentation:
  springdoc-openapi-starter-webmvc-ui: 2.2.0 (OpenAPI/Swagger)
  
Utilities:
  lombok: 1.18.30 (Reduce Boilerplate)
  mapstruct: 1.5.5 (Object Mapping)
  jackson-databind: JSON Processing
```

---

## 📦 Service Details

### 1. Eureka Server (Service Discovery)

**Purpose:** Central registry where all microservices register themselves.

**Key Responsibilities:**
- Service registration
- Service discovery
- Health monitoring
- Load balancer integration

**Main Class:**
```java
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

**application.yml:**
```yaml
server:
  port: 8761

spring:
  application:
    name: eureka-server

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
  server:
    enable-self-preservation: false
    eviction-interval-timer-in-ms: 10000
```

**Maven Dependencies:**
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
    </dependency>
</dependencies>
```

---

### 2. Config Server (Centralized Configuration)

**Purpose:** Externalize configuration from code to a central repository.

**Key Features:**
- Git-backed configuration
- Environment-specific properties
- Dynamic configuration refresh
- Encryption/Decryption support

**Main Class:**
```java
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

**application.yml:**
```yaml
server:
  port: 8888

spring:
  application:
    name: config-server
  cloud:
    config:
      server:
        git:
          uri: file://${user.home}/config-repo
          default-label: main
          clone-on-start: true
```

**Configuration Repository Structure:**
```
config-repo/
├── product-service.yml
├── order-service.yml
├── inventory-service.yml
├── notification-service.yml
├── api-gateway.yml
├── application.yml (common configs)
└── application-dev.yml (dev environment)
```

---

### 3. API Gateway (Spring Cloud Gateway)

**Purpose:** Single entry point for all microservices.

**Key Features:**
- Dynamic routing
- Load balancing
- Request/Response filtering
- Rate limiting
- Circuit breaking
- Authentication & Authorization

**Main Class:**
```java
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
```

**application.yml:**
```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
      routes:
        # Product Service Routes
        - id: product-service
          uri: lb://PRODUCT-SERVICE
          predicates:
            - Path=/api/products/**
          filters:
            - StripPrefix=1
            - name: CircuitBreaker
              args:
                name: productServiceCircuitBreaker
                fallbackUri: forward:/fallback/products
        
        # Order Service Routes
        - id: order-service
          uri: lb://ORDER-SERVICE
          predicates:
            - Path=/api/orders/**
          filters:
            - StripPrefix=1
            - name: CircuitBreaker
              args:
                name: orderServiceCircuitBreaker
        
        # Inventory Service Routes
        - id: inventory-service
          uri: lb://INVENTORY-SERVICE
          predicates:
            - Path=/api/inventory/**
          filters:
            - StripPrefix=1

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

**Custom Filters:**
```java
@Component
public class LoggingFilter implements GlobalFilter, Ordered {
    
    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("Request Path: {}", exchange.getRequest().getPath());
        return chain.filter(exchange);
    }
    
    @Override
    public int getOrder() {
        return -1;
    }
}
```

---

### 4. Product Service

**Purpose:** Manage product catalog and inventory information.

**Key Responsibilities:**
- CRUD operations for products
- Product search and filtering
- Category management
- SKU management

**Project Structure:**
```
product-service/
├── src/main/java/com/microservices/product/
│   ├── ProductServiceApplication.java
│   ├── controller/
│   │   └── ProductController.java
│   ├── service/
│   │   ├── ProductService.java
│   │   └── ProductServiceImpl.java
│   ├── repository/
│   │   └── ProductRepository.java
│   ├── model/
│   │   └── Product.java
│   ├── dto/
│   │   ├── ProductRequest.java
│   │   └── ProductResponse.java
│   ├── exception/
│   │   ├── ProductNotFoundException.java
│   │   └── GlobalExceptionHandler.java
│   └── config/
│       └── OpenApiConfig.java
└── src/main/resources/
    ├── application.yml
    └── db/migration/
        └── V1__create_products_table.sql
```

**Entity Class:**
```java
@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(length = 1000)
    private String description;
    
    @Column(nullable = false)
    private BigDecimal price;
    
    private String category;
    
    @Column(nullable = false, unique = true)
    private String skuCode;
    
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

**Controller:**
```java
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {
    
    private final ProductService productService;
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@RequestBody @Valid ProductRequest request) {
        log.info("Creating product with SKU: {}", request.getSkuCode());
        return productService.createProduct(request);
    }
    
    @GetMapping
    public List<ProductResponse> getAllProducts() {
        return productService.getAllProducts();
    }
    
    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }
    
    @GetMapping("/sku/{skuCode}")
    public ProductResponse getProductBySkuCode(@
