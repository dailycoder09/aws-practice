# Product Service

A standalone Spring Boot 3.2.1 service for managing a product catalog, built as a single consolidated application (not a microservice) to serve as the subject of an end-to-end AWS DevOps practice project.

## 🎯 Overview

The Product Service provides RESTful APIs for complete product lifecycle management including creation, retrieval, updates, deletion, and advanced search capabilities. It follows clean architecture principles with proper separation of concerns across layers, and ships with a 5,010-row seeded dataset across 10 categories so pagination, search, and filtering have real data to exercise from the first run.

## ✨ Features

### Core Functionality
- ✅ Complete CRUD operations for products
- ✅ Advanced search with multiple filters (name, category, price range)
- ✅ Pagination and sorting support
- ✅ SKU-based unique identification
- ✅ Category management
- ✅ Product count and existence checks

### Technical Excellence
- ✅ Clean layered architecture (Controller → Service → Repository → Database)
- ✅ Comprehensive input validation (Bean Validation JSR-303)
- ✅ Global exception handling with RFC 7807 Problem Details
- ✅ MapStruct for compile-time safe DTO mapping
- ✅ Database migrations with Flyway
- ✅ OpenAPI/Swagger documentation
- ✅ Comprehensive logging (console, file, JSON, error logs)
- ✅ Actuator endpoints for monitoring
- ✅ Prometheus metrics
- ✅ Docker containerization
- ✅ 80% line-coverage gate (JaCoCo) on the controller/service/repository packages

## 🏗️ Architecture

### Layered Architecture
```
┌─────────────────────────────────────┐
│         Controller Layer            │  REST endpoints, validation
├─────────────────────────────────────┤
│          Service Layer              │  Business logic
├─────────────────────────────────────┤
│        Repository Layer             │  Data access
├─────────────────────────────────────┤
│         Database (H2, file-based)   │  Data persistence
└─────────────────────────────────────┘
```

### Design Patterns Used
- **Repository Pattern**: Data access abstraction
- **DTO Pattern**: Request/Response separation from entities
- **Builder Pattern**: Clean object construction (Lombok)
- **Mapper Pattern**: Entity-DTO mapping (MapStruct)
- **Singleton Pattern**: Spring beans
- **Dependency Injection**: Constructor injection

## 🛠️ Technology Stack

| Category | Technology | Version |
|----------|-----------|---------|
| Framework | Spring Boot | 3.2.1 |
| Language | Java | 17 |
| Build Tool | Maven | 3.9+ |
| Database | H2 (file-based) | Runtime-managed |
| Migration | Flyway | Latest |
| Mapping | MapStruct | 1.5.5 |
| Documentation | Swagger/OpenAPI | 2.2.0 |
| Metrics | Prometheus (via Micrometer) | Latest |
| Testing | JUnit 5, Mockito, MockMvc | Latest |
| Code Coverage | JaCoCo | 0.8.11 |

## 📋 Prerequisites

- JDK 17 (see [Building without changing your default Java](#-building-without-changing-your-default-java) if your machine defaults to a different version)
- Maven 3.9+
- Docker (optional, only needed to build/run the container image)

No external database is required — H2 runs as an embedded file-based database and the schema/data are created automatically by Flyway on startup.

## 🚀 Getting Started

### Building without changing your default Java

If `java -version` / `mvn -v` on your machine resolves to something other than 17, use the bundled script instead of installing a new default JDK:

```bash
./build.sh          # mvn clean verify  - compiles, runs all tests + the JaCoCo gate
./build.sh package  # mvn clean package -DskipTests - fast jar build (what the Dockerfile does)
./build.sh run      # mvn spring-boot:run - starts the app on :8081
```

It points `JAVA_HOME` at a JDK 17 install for just that one process, so your system default is never touched. If `mvn`/`java` already resolve to 17 on your machine, the plain commands below work the same way.

### Build the Application

```bash
# Build with tests
mvn clean package

# Build without tests (faster)
mvn clean package -DskipTests
```

### Run the Application

```bash
# Using Maven
mvn spring-boot:run

# Using the packaged jar
java -jar target/product-service-1.0.0.jar

# With a specific profile
java -jar target/product-service-1.0.0.jar --spring.profiles.active=prod
```

On first run, Flyway creates the `products` table and loads the seed data (10 handwritten samples + 5,000 generated products across 10 categories) into a local H2 file at `./data/productdb` (configurable via the `DB_FILE_PATH` env var).

### Verify the Application

The application starts on port `8081`:

```bash
# Health check
curl http://localhost:8081/actuator/health

# Get all products (paginated)
curl http://localhost:8081/api/products
```

## 🖥️ Web UI

Open these in a browser (default port `8081`):

| Page | URL | What it shows |
|------|-----|---------------|
| Home | `http://localhost:8081/` | Which server answered the request: hostname, IP addresses, how the request arrived (client IP, `X-Forwarded-For`, host header), Java/OS/memory/uptime, app version and profile, and AWS EC2 details (instance ID, type, zone, region, IPs, AMI) when running on EC2 |
| Catalog | `http://localhost:8081/catalog` | A page of products with live stock from inventory-service (green above 20, amber at 20 or fewer, red at 0, grey when unknown) |

Notes for running in the cloud:
- The EC2 details come from the instance metadata service (IMDSv2) with a 500 ms timeout, so off-AWS the page simply says it isn't on EC2. Set `CLOUD_METADATA_ENABLED=false` to skip the lookup entirely.
- Inside Docker on EC2, the instance's metadata hop limit must be 2 (`http_put_response_hop_limit = 2` in Terraform), otherwise the container can't reach the metadata service and the EC2 section stays empty.
- The home page shows internal IPs and the instance ID to anyone who can reach the port. Fine for practice; in a real environment, limit access with the security group.
- The pages load IBM Plex fonts from Google Fonts. Without internet access in the viewer's browser they fall back to system fonts.
- Both pages can be previewed without running the app: open `src/main/resources/templates/index.html` or `catalog.html` directly in a browser (they contain sample values).

## 🐳 Docker

### Build the Image

```bash
docker build -t product-service:local .
```

### Run the Container

```bash
docker run -p 8081:8081 -v "$(pwd)/data:/data" product-service:local
```

A `docker-compose.yml` for the full local stack (app + monitoring) is planned for a later step and isn't in this repo yet.

## 📚 API Documentation

### Swagger UI
```
http://localhost:8081/swagger-ui.html
```

### OpenAPI Specification
```
http://localhost:8081/api-docs
```

### API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/products` | Create a new product |
| GET | `/api/products` | Get all products (paginated) |
| GET | `/api/products/{id}` | Get product by ID |
| GET | `/api/products/sku/{skuCode}` | Get product by SKU |
| PUT | `/api/products/{id}` | Update product |
| DELETE | `/api/products/{id}` | Delete product |
| GET | `/api/products/search` | Search products with filters |
| GET | `/api/products/category/{category}` | Get products by category |
| GET | `/api/products/categories` | Get all categories |
| GET | `/api/products/category/{category}/count` | Count products by category |
| GET | `/api/products/sku/{skuCode}/exists` | Check if SKU exists |

### Example Requests

#### Create Product
```bash
curl -X POST http://localhost:8081/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "iPhone 15 Pro",
    "description": "Latest Apple smartphone",
    "price": 999.99,
    "category": "Electronics",
    "skuCode": "IPH-15-PRO-001"
  }'
```

#### Get All Products
```bash
curl "http://localhost:8081/api/products?page=0&size=20&sortBy=name&sortDir=asc"
```

#### Search Products
```bash
curl "http://localhost:8081/api/products/search?searchTerm=pro&category=Electronics&minPrice=500&maxPrice=1500&page=0&size=10"
```

## 🌱 Seed Data

Flyway applies two migrations on startup:
- `V1__create_products_table.sql` — schema + 10 handwritten sample products
- `V2__seed_products.sql` — 5,000 deterministically-generated products (fixed RNG seed, so every fresh environment gets identical data) spread evenly across 10 categories: Electronics, Home Appliances, Clothing, Books, Sports and Outdoors, Toys and Games, Groceries, Furniture, Beauty and Personal Care, and Automotive

Total: 5,010 products, enough volume to meaningfully exercise pagination, search, and category filtering.

## 📊 Monitoring & Observability

### Actuator Endpoints
```
http://localhost:8081/actuator/health
http://localhost:8081/actuator/info
http://localhost:8081/actuator/metrics
http://localhost:8081/actuator/prometheus
```

### Logs
Logs are written to:
- **Console**: Colored output for development
- **File**: `${java.io.tmpdir}/product-service.log` (rolled daily, 10MB max per file)
- **JSON**: `${java.io.tmpdir}/product-service-json.log` (for centralized logging)
- **Error**: `${java.io.tmpdir}/product-service-error.log` (errors only)

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run with coverage (also enforced as a build gate by `mvn verify`)
mvn clean test jacoco:report
```

View the coverage report at `target/site/jacoco/index.html`.

The suite (48 tests) is pure JUnit 5 + Mockito + MockMvc + an embedded H2 database (`@DataJpaTest`) — no Docker or Testcontainers required to run it. The JaCoCo 80% line-coverage gate applies to the `controller`, `service`, and `repository` packages; generated/boilerplate code (`dto`, `exception`, `config`, `mapper`, `model`) is excluded by design.

## 🔧 Configuration Profiles

### Development Profile (`dev`, default)
- Verbose logging, SQL logging enabled

### Production Profile (`prod`)
- Optimized logging
- DB connection details overridable via `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` env vars

Activate a profile:
```bash
java -jar app.jar --spring.profiles.active=prod
```

The H2 web console is disabled in all profiles (`spring.h2.console.enabled: false`) — it's a known RCE vector (CVE-2021-42392) when left reachable.

## 📈 Performance Optimization

- **Connection Pooling**: HikariCP with optimized settings
- **Batch Processing**: Hibernate batch size = 20
- **Pagination**: Efficient large dataset handling
- **Async Logging**: Non-blocking log appenders
- **JVM Tuning**: G1GC with optimized heap settings (see Dockerfile)

## 🔒 Security

- Non-root Docker user
- Input validation on all endpoints
- SQL injection prevention (prepared statements / JPA)
- Error message sanitization
- H2 console disabled in every profile

## 📝 Code Quality

- **Lombok**: Reduces boilerplate
- **MapStruct**: Type-safe mapping
- **JaCoCo**: 80% coverage gate on controller/service/repository, enforced in `mvn verify`

## 🤝 Contributing

1. Follow clean code principles
2. Write tests for new features
3. Update documentation
4. Follow existing code style
5. Create meaningful commit messages

## 📄 License

This project is licensed under the MIT License.

---

**Version**: 1.0.0
**Last Updated**: 2026-10-04
