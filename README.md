# MyEnterpriseOpenSource

Backend application for a B2B inventory, warehouse and order management system built with Java and Spring Boot.

The goal of this project is to develop a production-oriented backend while practicing database design, transactions, concurrency, testing and backend architecture.

> 🚧 **Work in progress**

## Tech Stack

- Java
- Spring Boot
- Spring Data JPA / Hibernate
- Flyway
- MySQL
- Docker
- Maven
- Bean Validation
- Lombok

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

Sensitive configuration such as database passwords is not stored directly in the repository.

The application reads credentials through environment variables.

Files such as:

```text
.env
```

are excluded through `.gitignore`.

Future security work will include:

- Spring Security
- Authentication
- Authorization
- Role-based access control
- Multi-tenant resource protection

## Roadmap

Planned development includes:

- Service layer
- Business rules
- REST API
- Request and response DTOs
- API validation
- Exception handling
- Transactional stock reservation
- Order state transitions
- Stock transfers between warehouses
- Inventory movement auditing
- Prevention of overselling
- Concurrent request testing
- Authentication and authorization
- Multi-tenant isolation
- Integration testing
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
