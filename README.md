# MyEnterpriseOpenSource

Backend application for a B2B inventory, warehouse and order management system built with Java and Spring Boot.

The goal of this project is to develop a production-oriented backend while practicing database design, transactions, concurrency, testing and backend architecture.

> 🚧 **Work in progress**

## Tech Stack

- Java 21
- Spring Boot
- Spring Data JPA / Hibernate
- Flyway
- MySQL
- Maven
- Bean Validation

## Current Features

The project currently includes:

- Relational database schema managed with Flyway
- Company management domain
- Users with roles and company ownership
- Products with company-specific SKUs
- Warehouses
- Inventory by product and warehouse
- Sales orders and order items
- Stock reservations
- Shipments and shipment items
- Inventory movement history
- JPA entity mappings
- Spring Data JPA repositories
- REST endpoints for companies, products, warehouses, inventory, orders and shipments
- Request validation and consistent HTTP errors
- Transactional stock reservation, partial shipments and movement history
- Automated unit and integration tests

The eleven entities and repositories are under `com.myenterpriseos.myenterpriseopensource` so Spring discovers them automatically.

## API

| Method | Path | Purpose |
|---|---|---|
| POST / GET | `/api/companies/{companyId}/products` | Create or list products |
| POST / GET | `/api/companies/{companyId}/warehouses` | Create or list warehouses |
| POST / GET | `/api/warehouses/{warehouseId}/inventory` | Receive stock or list inventory |
| POST | `/api/inventory/{inventoryId}/adjustments` | Adjust stock |
| GET | `/api/inventory/{inventoryId}/movements` | View stock history |
| POST | `/api/companies/{companyId}/orders` | Create a draft order |
| GET | `/api/orders/{orderId}` | View an order |
| POST | `/api/orders/{orderId}/confirm` | Allocate and reserve all order lines |
| POST | `/api/orders/{orderId}/cancel` | Cancel an unshipped order |
| POST | `/api/orders/{orderId}/shipments` | Ship reserved items, including partial shipments |

Confirmation requires `allocations` with `orderItemId`, `inventoryId`, and `quantity` for every order line. A shipment requires `warehouseId` and `items` with `orderItemId` and `quantity`. The service checks company ownership, available stock and order state inside database transactions. Inventory rows are locked while reserving or changing stock.

All API endpoints except login require a bearer JWT. The security layer checks the account and its current role against the database on every request. Requests cannot access a different company by changing an ID in the URL.

## Authentication and authorization

JWTs are signed with an externally configured RSA key pair of at least 3072 bits. The server validates the signature, issuer, audience and expiration. Access tokens last 15 minutes. Changing a password or deactivating an account invalidates its existing tokens immediately. Five failed logins lock an account for 15 minutes. Passwords use BCrypt; new passwords must contain at least 14 characters and fit within BCrypt's 72-byte UTF-8 limit.

Generate a PKCS#8 RSA private key and matching public key, for example:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out jwt-private.pem
openssl pkey -in jwt-private.pem -pubout -out jwt-public.pem
```

Keep the private key outside the repository with restricted file permissions. Set `JWT_PRIVATE_KEY_LOCATION` and `JWT_PUBLIC_KEY_LOCATION` to Spring resource locations (for example `file:C:/secure/jwt-private.pem`) and set `JWT_ISSUER` to a stable HTTPS issuer URI. The application fails to start if the keys or issuer are missing or the key pair is invalid. The PEM files under `src/test/resources` are test-only fixtures and must never be used outside tests.

There is no public registration endpoint. To create the first administrator, start the application once with `APP_SECURITY_BOOTSTRAP_ENABLED=true`, `BOOTSTRAP_COMPANY_NAME`, `BOOTSTRAP_ADMIN_USERNAME` and `BOOTSTRAP_ADMIN_PASSWORD`. For an existing company, use `BOOTSTRAP_COMPANY_ID` instead of the name. Remove the bootstrap variables after the account is created. Bootstrap only runs if no users exist.

Login with `POST /api/auth/login` using `companyId`, `username` and `password`. Send the returned token as `Authorization: Bearer <accessToken>`. Administrators can create users with `POST /api/users` and deactivate them with `POST /api/users/{userId}/deactivate`; authenticated users can change their own password with `POST /api/users/change-password`.

| Role | Allowed changes |
|---|---|
| `ADMIN` | Products, warehouses, inventory, orders, shipments and users |
| `WAREHOUSE_MANAGER` | Warehouses, inventory and shipments |
| `SALES` | Orders and reservations |
| `VIEWER` | Read-only access within its company |

## Automated tests

Run `./mvnw test` (or `mvn test` on Windows). Tests use an in-memory H2 database in MySQL mode and apply the real Flyway migrations. They create and roll back their own data; no sample records or external MySQL server are needed. Unit tests use mocked repositories to check business rules without starting a database. Security integration tests issue real JWTs and check login, role permissions, company isolation, password changes, account deactivation and lockout.

## Domain Overview

The application is designed around multiple companies using the same backend.

Main domain entities:

- `Company`
- `AppUser`
- `Product`
- `Warehouse`
- `Inventory`
- `SalesOrder`
- `OrderItem`
- `StockReservation`
- `Shipment`
- `ShipmentItem`
- `InventoryMovement`

The database contains constraints and relationships to protect important business invariants such as:

- Positive order quantities
- Non-negative inventory
- Unique product/warehouse inventory combinations
- Unique SKU per company
- Referential integrity between entities

## Project Structure

```text
src/main/java/com/myenterpriseos/myenterpriseopensource
├── entity
├── enums
├── repository
├── service
├── controller
└── dto
```

Database migrations are located in:

```text
src/main/resources/db/migration
```

## Configuration

Database credentials are provided through environment variables.

Example `.env`:

```env
DB_URL=jdbc:mysql://localhost:3307/myenterpriseos
DB_USERNAME=root
DB_PASSWORD=your_password
```

The real `.env` file is excluded from Git and should never be committed.

Spring uses the environment variables through `application.properties`:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
spring.jpa.show-sql=true
```

## Running the Project

Make sure MySQL is running and the required environment variables are configured.

Then run:

```bash
./mvnw spring-boot:run
```

Flyway automatically applies pending database migrations when the application starts.

Hibernate is configured with:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

This means Hibernate validates the JPA entities against the existing database schema but does not create or modify the database structure.

Database schema changes are managed through Flyway migrations.

## Database Migrations

Flyway migrations are stored in:

```text
src/main/resources/db/migration
```

Example:

```text
V1__initial_schema.sql
V2__app_user.sql
V3__product.sql
```

Flyway keeps track of executed migrations using its internal:

```text
flyway_schema_history
```

table.

## Main Domain Relationships

The current domain model contains relationships such as:

```text
Company
├── AppUser
├── Product
├── Warehouse
└── SalesOrder
```

Inventory connects products with warehouses:

```text
Product
   │
   ▼
Inventory
   ▲
   │
Warehouse
```

Orders contain order lines:

```text
SalesOrder
    │
    ▼
OrderItem
    │
    ▼
Product
```

Stock reservations connect order items with inventory:

```text
OrderItem
     │
     ▼
StockReservation
     │
     ▼
Inventory
```

Shipments support partial order fulfillment:

```text
SalesOrder
    │
    ▼
Shipment
    │
    ▼
ShipmentItem
    │
    ▼
OrderItem
```

Inventory changes are tracked through:

```text
Inventory
    │
    ▼
InventoryMovement
```

## Business Rules

Some of the business invariants currently protected by the database include:

### Inventory

Inventory quantity cannot be negative.

```text
quantity >= 0
```

Only one inventory record can exist for the same product and warehouse combination.

```text
UNIQUE(product_id, warehouse_id)
```

### Products

SKUs are unique inside each company.

```text
UNIQUE(company_id, sku)
```

This allows different companies to use the same SKU while preventing duplicates inside the same company.

### Order Items

Order quantities must be greater than zero.

```text
quantity > 0
```

Prices cannot be negative.

```text
price >= 0
```

### Stock Reservations

Reservation quantities must be greater than zero.

```text
quantity > 0
```

A reservation connects a specific `OrderItem` with a specific `Inventory` record.

### Shipment Items

Shipment quantities must be greater than zero.

```text
quantity > 0
```

### Inventory Movements

Inventory movements cannot have a zero quantity change.

Examples:

```text
+10 INITIAL_STOCK
+5  ADJUSTMENT_IN
-3  ADJUSTMENT_OUT
-8  SHIPMENT
+7  TRANSFER_IN
-7  TRANSFER_OUT
```

## Money Representation

Monetary values are represented using:

```java
BigDecimal
```

instead of floating-point types such as:

```java
double
float
```

The database stores monetary values using:

```sql
DECIMAL(12,2)
```

This avoids precision problems associated with floating-point arithmetic.

## JPA Relationships

Relationships are generally configured using lazy loading:

```java
@ManyToOne(fetch = FetchType.LAZY)
```

This prevents Hibernate from automatically loading related entities when they are not required.

Relationships that are mandatory are mapped using:

```java
@ManyToOne(
    fetch = FetchType.LAZY,
    optional = false
)

@JoinColumn(
    name = "company_id",
    nullable = false
)
```

The database stores foreign keys as IDs, while JPA exposes the related entity directly.

For example:

```text
Database:

stock_reservation.order_item_id = 15
```

is represented in Java as:

```java
private OrderItem orderItem;
```

The ID can still be accessed through:

```java
stockReservation.getOrderItem().getId();
```

## Project Architecture

The application follows a layered backend architecture:

```text
HTTP Request
     │
     ▼
Controller
     │
     ▼
Service
     │
     ▼
Repository
     │
     ▼
JPA / Hibernate
     │
     ▼
MySQL
```

Responsibilities are separated between layers.

### Entity

Represents the persisted domain model.

### Repository

Provides database access using Spring Data JPA.

Example:

```java
public interface CompanyRepository
        extends JpaRepository<Company, Long> {
}
```

### Service

Contains business logic and transaction boundaries.

### Controller

Exposes REST endpoints.

### DTO

Defines API input and output models without directly exposing JPA entities.

## Security

Database credentials and JWT signing keys are read from environment variables and files outside the repository. `.env` and PEM files are excluded through `.gitignore`. Run the API behind HTTPS in production; bearer tokens must not be sent over plain HTTP.

## Roadmap

Planned development includes:

- Stock transfers between warehouses
- Concurrent request testing
- Testcontainers
- Docker Compose environment
- Advanced SQL queries
- Database indexes
- `EXPLAIN ANALYZE`
- N+1 query optimization
- Redis caching
- Asynchronous messaging
- RabbitMQ or Kafka
- Idempotent event processing
- Database/broker consistency
- Observability
- Metrics
- Structured logging

## Main Goal

This project is being developed as a backend engineering portfolio project.

The focus is not only on implementing CRUD operations, but on understanding and solving real backend engineering problems such as:

- Data consistency
- Database design
- Constraints
- Transactions
- Concurrency
- Overselling prevention
- Multi-tenancy
- Application architecture
- Testing
- SQL performance
- Infrastructure
- Distributed systems

The objective is to build a backend whose technical decisions can be explained and defended rather than simply accumulating features.

## Status

🚧 **Currently under active development.**
