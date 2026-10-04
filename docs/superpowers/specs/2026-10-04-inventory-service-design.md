# Inventory Service: a second, independent Spring Boot project that talks to product-service

## Context

`product-service` was deliberately consolidated into a single Spring Boot application — the original 8-service microservices vision was abandoned in favor of one solid, well-tested app, specifically to keep the AWS DevOps practice project's scope manageable.

This spec reopens that boundary on purpose: the goal now is to build a second, independent Spring Boot service (`inventory-service`) and have it communicate with `product-service`, as a deliberate exercise in inter-service communication and failure handling — not a reversal of the consolidation decision, but an intentional expansion once the single-service foundation was solid.

## Goals

- Two independently buildable/runnable/deployable Spring Boot projects, each with its own database, each testable in isolation.
- Real, working HTTP communication in both directions, each teaching a different resilience pattern:
  - **Inventory → Product** (write-path validation): creating a stock record requires confirming the SKU exists in the catalog.
  - **Product → Inventory** (read-path enrichment): fetching a single product optionally shows live stock, degrading gracefully if inventory-service is unavailable.
- Keep the same quality bar established in `product-service`: layered architecture, Flyway migrations, Bean Validation, RFC 7807 `ProblemDetail` error responses, Actuator + Prometheus, OpenAPI/Swagger, JaCoCo 80% gate on real logic, H2 console disabled, the `NoResourceFoundException` → 404 fix included from the start.
- An audit trail of stock changes: every create/adjust/update/delete on an inventory record writes an immutable log entry (SKU, change type, previous → new quantity, timestamp), readable per SKU.
- Full data consistency with product-service: every one of the 5,010 seeded product SKUs gets a matching inventory record (and a seeded "CREATED" audit entry) — no subset, no intentional gaps.

## Non-Goals (explicitly deferred, YAGNI)

- No service discovery (Eureka) or config server — direct configurable URLs, consistent with the earlier consolidation decision.
- No message broker / async communication (Kafka, RabbitMQ) — synchronous REST only.
- No circuit breaker library (Resilience4j) in this pass — timeouts + explicit error mapping only. Flagged as a natural follow-up once this integration is proven.
- No stock enrichment on list/search/pagination endpoints — only the two single-item product reads (`getProductById`, `getProductBySkuCode`). Enriching a paginated list would require N calls per request (or a batch endpoint), which is materially more complexity than this pass needs.
- No Testcontainers/WireMock — `MockRestServiceServer` (already available via `spring-boot-starter-test`) covers HTTP-client-level testing without a new dependency.
- No AWS/Terraform/CI changes yet — this spec covers the two applications and their communication only. Deploying both to AWS is a separate, later step in the existing roadmap.

## Architecture

```
┌─────────────────────┐        GET /api/products/sku/{sku}        ┌──────────────────────┐
│                      │ ─────────────────────────────────────────▶│                      │
│   inventory-service  │        (validate SKU exists, write path)   │   product-service     │
│   :8082, H2 file DB  │                                             │   :8081, H2 file DB  │
│                      │◀───────────────────────────────────────── │                      │
└──────────────────────┘        GET /api/inventory/sku/{sku}        └──────────────────────┘
                                 (stock lookup, read-path enrichment)
```

Each service owns its own H2 file database — no shared schema, no DB-level foreign key between them. The only coupling is the two REST calls below, each resolved via a configurable base URL (no service discovery).

| Config property | Env var | Default | Used by |
|---|---|---|---|
| `inventory.product-service.url` | `PRODUCT_SERVICE_URL` | `http://localhost:8081` | inventory-service |
| `product.inventory-service.url` | `INVENTORY_SERVICE_URL` | `http://localhost:8082` | product-service |

Both clients use a 2s connect / 3s read timeout.

## Components

### `inventory-service` (new project, sibling to `product-service`)

Mirrors `product-service`'s package layout exactly:

```
inventory-service/
├── Dockerfile, build.sh, pom.xml, README.md
└── src/main/java/com/microservices/inventory/
    ├── InventoryServiceApplication.java
    ├── client/
    │   ├── ProductClient.java          (interface: Optional<ProductSummary> findBySkuCode(String) - "exists" is just isPresent())
    │   └── ProductClientImpl.java      (RestClient-based; maps 404 → Optional.empty(), network/timeout/5xx → throws ProductServiceUnavailableException)
    ├── config/OpenApiConfig.java, RestClientConfig.java
    ├── controller/InventoryController.java
    ├── dto/request/InventoryRequest.java, AdjustStockRequest.java
    ├── dto/response/InventoryResponse.java, ProductSummary.java (minimal: skuCode, name, price - just enough to deserialize what's needed from product-service)
    ├── exception/
    │   ├── SkuNotFoundInCatalogException   (422 - SKU doesn't exist in product-service)
    │   ├── DuplicateInventoryException      (409 - inventory record for this SKU already exists)
    │   ├── InventoryNotFoundException       (404)
    │   ├── ProductServiceUnavailableException (503 - product-service unreachable/erroring)
    │   └── GlobalExceptionHandler            (RFC 7807 ProblemDetail, same pattern as product-service, includes NoResourceFoundException → 404 from day one)
    ├── mapper/InventoryMapper.java (MapStruct)
    ├── model/InventoryItem.java, InventoryAuditLog.java
    ├── repository/InventoryRepository.java, InventoryAuditLogRepository.java
    └── service/InventoryService.java, service/impl/InventoryServiceImpl.java
```

**`InventoryItem` entity**: `id`, `skuCode` (unique, not a DB foreign key - validity enforced via the REST call, not SQL), `quantityOnHand` (>= 0), `reorderThreshold` (default 10), `createdAt`/`updatedAt` (`@CreationTimestamp`/`@UpdateTimestamp`, same as `Product`).

**`InventoryAuditLog` entity** (append-only, no updates/deletes to this table itself): `id`, `skuCode`, `changeType` (`CREATED` / `ADJUSTED` / `UPDATED` / `DELETED`), `previousQuantity` (nullable - null for `CREATED`), `newQuantity` (nullable - null for `DELETED`), `changedAt`. Written inside the same `@Transactional` boundary as the `InventoryItem` write it describes, so the two can never drift apart - if the audit insert fails, the whole operation rolls back.

**Endpoints**:
| Method | Path | Behavior |
|---|---|---|
| POST | `/api/inventory` | Create a stock record. Calls product-service first: 422 if SKU unknown, 409 if a record for that SKU already exists, 503 if product-service unreachable. Writes a `CREATED` audit entry. |
| GET | `/api/inventory/{id}` | 404 if missing |
| GET | `/api/inventory/sku/{skuCode}` | 404 if missing. This is what product-service calls for enrichment. |
| GET | `/api/inventory` | Paginated list |
| PUT | `/api/inventory/{id}` | Replace quantity/threshold. Writes an `UPDATED` audit entry. |
| PATCH | `/api/inventory/sku/{skuCode}/adjust?delta=-5` | Increment/decrement; 400 if result would go negative. Writes an `ADJUSTED` audit entry. |
| DELETE | `/api/inventory/{id}` | 404 if missing. Writes a `DELETED` audit entry. |
| GET | `/api/inventory/sku/{skuCode}/audit` | Paginated audit history for a SKU, most recent first |

**Seed data** (`V2__seed_inventory.sql`, after `V1__create_inventory_table.sql` creates the schema): stock records for **all 5,010** product-service SKUs — the 10 handwritten V1 ones plus all 5,000 V2 bulk-generated ones, regenerated deterministically with the same category/prefix scheme and RNG seed product-service used, so the SKU codes line up exactly. Quantities are randomized (mostly 1-500, a small fraction forced to `0` for a realistic out-of-stock case) with a fixed seed for reproducibility. Each seeded row also gets a matching `CREATED` audit log entry, so the audit trail is complete from the first startup, not just for records created later through the API. This fully matches product-service's catalog — no product exists without a corresponding inventory record.

The read-path graceful-degradation behavior (product-service tolerating a missing/unreachable inventory record) is still implemented and still real — it now simply has no seeded case that exercises "legitimately missing" by default. It still applies if a SKU is deleted from inventory-service (`DELETE /api/inventory/{id}`) while the product still exists, or if inventory-service is down.

### `product-service` (existing project, small additive change)

- New `client/InventoryClient.java` + `InventoryClientImpl.java` (same `RestClient` pattern): `Optional<Integer> findStockBySkuCode(String)` — returns empty on 404, network failure, *or* timeout (never throws out to the caller).
- `ProductResponse` gets one new nullable field: `stockQuantity` (Integer).
- `ProductServiceImpl.getProductById` / `getProductBySkuCode` call the new client after mapping the entity, setting `stockQuantity` if present, leaving it `null` otherwise. No other methods change.
- No new exceptions needed here — a failed inventory lookup is swallowed into `null`, by design (read-path degrades, never fails the product read).

**Note on the asymmetry**: `ProductClient` (inventory-service) throws on network/timeout/5xx, while `InventoryClient` (product-service) swallows the same failures into `Optional.empty()`. This is intentional, not inconsistent — it's the direct consequence of one call being a write-path validation (must fail loudly if it can't be checked) and the other being a read-path enrichment (must never break the primary read). Worth a one-line comment on each client class so a future reader doesn't "fix" the asymmetry.

## Error Handling Summary

| Scenario | Inventory-service behavior | Product-service behavior |
|---|---|---|
| Downstream SKU doesn't exist | 422 `SkuNotFoundInCatalogException` on `POST /api/inventory` | n/a |
| Downstream service down/timeout, write path | 503 `ProductServiceUnavailableException` on `POST /api/inventory` | n/a |
| Downstream service down/timeout, read path | n/a | `stockQuantity: null`, 200 OK |
| Downstream record doesn't exist, read path | n/a | `stockQuantity: null`, 200 OK |

## Testing Strategy

Same layered approach as `product-service`: Mockito unit tests for service logic (repository **and** the relevant client interface both mocked — no real HTTP calls in the unit suite), `@WebMvcTest` + MockMvc for controllers, `@DataJpaTest` + embedded H2 for repositories. The two `*ClientImpl` classes get their own tests using Spring's `MockRestServiceServer` (via `spring-boot-starter-test`, no new dependency) to verify the real request construction and response/error mapping without a live server. JaCoCo 80% gate applies to `controller`/`service`/`repository`/`client` (the client wrapper contains real error-mapping logic worth covering); `dto`/`exception`/`config`/`mapper`/`model` excluded, matching `product-service`'s existing exclusion rationale.

Audit coverage specifically: service-layer tests assert that each of create/adjust/update/delete produces exactly one correctly-shaped audit entry (right `changeType`, right previous/new quantities), and a repository test confirms the 5,010 seeded rows have matching seeded `InventoryItem` + `InventoryAuditLog` rows (counts line up, spot-check a few SKUs).

A manual end-to-end smoke test (both services actually running, hitting the real endpoints) proves the live integration, the same way `product-service`'s CRUD and seed data were verified earlier in this project.

## Open Questions / Explicit Future Enhancements (not in this pass)

- Resilience4j circuit breaker + retry on both clients, once the plain version is proven.
- A batch stock-lookup endpoint if list/search enrichment is ever wanted.
- Docker Compose wiring both services together (depends on the still-pending Milestone 2 "local dev loop" work from the main AWS roadmap).
- Deploying both services to AWS (depends on the still-pending Terraform/EC2 milestones).

## Note on repo state

This repo has no git history yet (`git init` is still pending from the main roadmap's "Housekeeping" milestone). This spec is written to disk but not committed; it should be committed once git is initialized.
