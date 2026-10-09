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
- ✅ Rich product details like an e-commerce listing/detail page: brand, MRP with a derived discount, highlights, grouped specifications, warranty, seller and an image gallery (see [Product details](#-product-details))

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

On first run, Flyway creates the `products` table and loads the seed data (10 handwritten samples + 5,000 generated products across 10 categories, each with brand/MRP/highlights/specifications/images) into a local H2 file at `./data/productdb` (configurable via the `DB_FILE_PATH` env var).

### Verify the Application

The application starts on port `8081`:

```bash
# Health check
curl http://localhost:8081/actuator/health

# Get all products (paginated)
curl http://localhost:8081/api/products
```

## 🖥️ Server info endpoint

```bash
curl http://localhost:8081/api/products/server-info
```

Returns JSON describing which server answered the request: `application` (name, version, profiles, port), `host` (hostname, IP addresses, whether it runs in a container), `request` (how the request arrived: host header, client IP, `X-Forwarded-For`), `runtime` (Java/OS/CPU/memory/uptime), and `cloud` (instance ID, type, zone, region, IPs, AMI) only when running on AWS EC2. The response is sent with `Cache-Control: no-store`, so behind a load balancer every call reflects the instance that actually served it.

It is meant for the React UI at `../ui-app`, which renders the details; product-service no longer serves any HTML pages. The path sits under `/api/products/` so the load balancer's `/api/products*` rule routes it to this service.

Notes for running in the cloud:
- The EC2 details come from the instance metadata service (IMDSv2) with a 500 ms timeout, so off-AWS the `cloud` field is simply left out. Set `CLOUD_METADATA_ENABLED=false` to skip the lookup entirely.
- Inside Docker on EC2, the instance's metadata hop limit must be 2 (`http_put_response_hop_limit = 2` in Terraform), otherwise the container can't reach the metadata service and `cloud` stays empty.
- The endpoint exposes internal IPs and the instance ID to anyone who can reach the port. Fine for practice; in a real environment, limit access with the security group.

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
| GET | `/api/products/{id}/details` | Full product details by ID (images, highlights, grouped specifications) |
| GET | `/api/products/sku/{skuCode}/details` | Full product details by SKU |
| PUT | `/api/products/{id}/images` | Replace the whole image list of a product (image URLs only) |
| PUT | `/api/products/{id}` | Update product |
| DELETE | `/api/products/{id}` | Delete product |
| GET | `/api/products/search` | Search products with filters |
| GET | `/api/products/category/{category}` | Get products by category |
| GET | `/api/products/categories` | Get all categories |
| GET | `/api/products/category/{category}/count` | Count products by category |
| GET | `/api/products/sku/{skuCode}/exists` | Check if SKU exists |
| GET | `/api/products/server-info` | Details of the server instance that handled the request |

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

## 🛍️ Product details

Products carry the extra data an e-commerce listing/detail page needs. Only details and images are covered: there are no reviews/ratings, no extra search filters and no related-products.

### Two response shapes

| Shape | Used by | Contents |
|-------|---------|----------|
| `ProductResponse` (light) | list, search, category, create, update, `GET /{id}`, `GET /sku/{sku}` | the original fields plus `brand`, `mrp`, `discountPercent`, `imageUrl`. **No** images/highlights/specifications arrays, and list queries never touch the child tables, so there is no N+1. |
| `ProductDetailResponse` (full) | `GET /{id}/details`, `GET /sku/{sku}/details`, `PUT /{id}/images` | `id, name, description, price, mrp, discountPercent, category, brand, skuCode, warranty, seller, stockQuantity, images[], highlights[], specifications[], createdAt, updatedAt` |

All new fields are optional and omitted from the JSON when null (the app-wide `non_null` setting). `stockQuantity` is the one exception and is always present (null when unknown). On the detail endpoints it is filled from inventory-service exactly like `GET /{id}` does and fails open (null when inventory-service has no record or is unreachable).

```bash
curl http://localhost:8081/api/products/sku/APPLE-IP15P-128/details
```
```json
{
  "id": 1, "name": "iPhone 15 Pro", "price": 999.99, "mrp": 1099.99, "discountPercent": 9,
  "category": "Electronics", "brand": "Apple", "skuCode": "APPLE-IP15P-128",
  "warranty": "1 year limited warranty", "seller": "Orbit Electronics Retail",
  "stockQuantity": null,
  "images": [
    { "url": "https://placehold.co/800x800/172740/E6EDF7/png?text=APPLE-IP15P-128+1", "alt": "iPhone 15 Pro - view 1" }
  ],
  "highlights": ["A17 Pro chip with a 6-core GPU ...", "..."],
  "specifications": [
    { "group": "Display", "items": [ { "key": "Screen Size", "value": "6.1 inches" } ] },
    { "group": "Performance", "items": [ { "key": "Chip", "value": "A17 Pro" } ] }
  ]
}
```

Specifications are stored as flat rows and grouped on read: groups appear in the order of their first row, items keep their `sort_order`.

### Discount is derived, never stored

`discountPercent = round((mrp - price) / mrp * 100)` as a whole number (half-up), recalculated on every read in `DiscountCalculator`. It is `null` when `mrp` is null or `mrp <= price` (nothing to show). Changing `price` or `mrp` therefore never leaves a stale discount behind.

### Writing details (create / update)

`POST /api/products` and `PUT /api/products/{id}` accept these optional fields in addition to the existing ones:

| Field | Rules |
|-------|-------|
| `brand`, `warranty`, `seller` | max 100 characters |
| `mrp` | 0.01 - 999999.99, at most 2 decimals. Must not be lower than `price` when both are sent, otherwise `400` ("MRP cannot be lower than the price") |
| `highlights` | list of up to 10 strings, each not blank, max 200 characters |
| `specifications` | list of up to 50 `{ "group", "key", "value" }` (group/key max 100, value max 255, none blank) |

Update semantics: a field that is **absent/null is left unchanged**; a provided `highlights`/`specifications` list **replaces** the stored list; an **empty list clears** it. Images are not part of the product request - use the images endpoint below.

### Images are URLs only

There is no upload, no file storage and no S3: the service only stores image URLs. `PUT /api/products/{id}/images` **replaces the whole list** (0-10 images, an empty list removes all of them) and sets the product's `imageUrl` to the first image's URL (or null):

```bash
curl -X PUT http://localhost:8081/api/products/1/images \
  -H "Content-Type: application/json" \
  -d '{"images":[{"url":"https://cdn.example.com/iphone-front.jpg","alt":"iPhone 15 Pro front"},
                 {"url":"https://cdn.example.com/iphone-back.jpg","alt":"iPhone 15 Pro back"}]}'
```

Validation (violations return the usual `400` problem-detail with an `errors` map): `images` is required and holds at most 10 entries; each `url` is required, max 500 characters and must match `^https?://\S+$` (**http/https only** - the stored URLs end up in `<img src>`, so `javascript:`, `data:`, `ftp:` and relative URLs are rejected); `alt` is optional, max 255. Unknown product id gives `404`.

### Dummy images and how to replace them

Every seeded product has 4 **dummy** placeholder images of the form `https://placehold.co/800x800/<bg>/E6EDF7/png?text=<SKU>+<n>` (`n` = 1-4, `bg` one of four dark colours). They need internet access to render and exist only so a UI has something to show. To use real pictures:
- **Running environment**: call `PUT /api/products/{id}/images` per product with the real URLs (above).
- **New environments / fresh databases**: edit the images section at the end of `V4__seed_product_details.sql` (the `INSERT INTO product_images ...` and the `UPDATE products SET image_url ...` statements) before the database is first created. Never edit a migration that has already run somewhere - Flyway checksums would fail; use a new `V5__...sql` migration for that.

## 🌱 Seed Data

Flyway applies four migrations on startup:
- `V1__create_products_table.sql` — schema + 10 handwritten sample products
- `V2__seed_products.sql` — 5,000 deterministically-generated products (fixed RNG seed, so every fresh environment gets identical data) spread evenly across 10 categories: Electronics, Home Appliances, Clothing, Books, Sports and Outdoors, Toys and Games, Groceries, Furniture, Beauty and Personal Care, and Automotive
- `V3__product_details_schema.sql` — new `brand`, `mrp`, `warranty`, `seller`, `image_url` columns on `products` (all nullable, `mrp >= 0` check, index on `brand`) and the `product_images`, `product_highlights`, `product_specifications` tables (`ON DELETE CASCADE` to `products`, indexed on `product_id`, image position unique per product)
- `V4__seed_product_details.sql` — details for all 5,010 products. The 10 original products have hand-written brand, MRP, warranty, seller, 5 highlights, 8-9 specifications in 3 groups and 4 images. The 5,000 generated products get a brand from a per-category pool of 6 made-up names, an MRP of price x 1.10-1.60 (never below price), a per-category warranty/seller, 3-5 highlights, 6-8 specifications in 2 groups from per-category templates, and 4 dummy images. It is deterministic (no RNG, everything derives from `MOD(id, n)`) and written set-based (`MERGE` / `INSERT ... SELECT` from small `VALUES` template tables) rather than one `INSERT` per row, so it stays fast on every fresh start.

Total: 5,010 products, enough volume to meaningfully exercise pagination, search, and category filtering. Deleting a product removes its images, highlights and specifications (JPA cascade plus the foreign keys' `ON DELETE CASCADE`).

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

The suite (221 tests, up from 79 before product details) is pure JUnit 5 + Mockito + MockMvc + an embedded H2 database (`@DataJpaTest`) — no Docker or Testcontainers required to run it. The JaCoCo 80% line-coverage gate applies to the `controller`, `service`, `service.impl`, `client`, and `repository` packages; generated/boilerplate code (`dto`, `exception`, `config`, `mapper`, `model`) is excluded by design (the mapper and discount calculation are still unit-tested).

What the product-details tests cover:
- **Service unit tests** (`ProductServiceImplTest`): details by id/SKU with and without stock, not found, image replacement (primary `imageUrl` set/cleared, delete flushed before re-insert), create/update with the new fields (collections replaced, null leaves unchanged, empty list clears, MRP below price rejected).
- **Mapper/discount tests** (`ProductMapperTest`, `DiscountCalculatorTest`): derived discount edge cases (null MRP, MRP equal to price, rounding), the light response never touches the lazy collections, specification grouping.
- **Controller tests** (`ProductControllerTest`, `@WebMvcTest`): the new endpoints and JSON shape, `/details` vs `/{id}` and `/sku/{sku}` routing, image-URL validation (more than 10 images, blank, `javascript:`, `ftp:`, `data:` ...), validation of the new `ProductRequest` fields.
- **Repository tests** (`ProductDetailsRepositoryTest`, Flyway V1-V4 on embedded H2): all 5,010 products have a brand, an MRP >= price, an `image_url`, exactly 4 images, >= 3 highlights and >= 6 specification rows; the 10 original SKUs keep their hand-written brand; deleting a product cascades.
- **Service integration tests** (`ProductServiceIntegrationTest`): real service + mapper + database for image replacement, create/update semantics, delete cascade, and that list/search queries load no collections (no N+1).

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
**Last Updated**: 2026-10-09
