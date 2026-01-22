# E-Commerce Microservices System

A scalable microservices-based e-commerce system built with Spring Boot, featuring inventory management and order processing with real-time availability checking.

##  Architecture Overview

This project consists of two microservices:

1. **Inventory Service** (Port 8081) - Manages product inventory with batch tracking and expiry dates
2. **Order Service** (Port 8082) - Handles order placement and communicates with Inventory Service

### Key Design Patterns

- **Factory Design Pattern**: Implemented in Inventory Service for extensible inventory handling strategies (FIFO, LIFO)
- **Layered Architecture**: Controller → Service → Repository pattern in both services
- **RESTful Communication**: Inter-service communication via REST APIs using RestTemplate

##  Prerequisites

- Java 17 or higher
- Git
- No need to install Gradle (uses Gradle Wrapper)

##  Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/Vishal-21-lang/supplychain-inventory-orchestrator.git
cd Testing
```

### 2. Build Both Services

```bash
# Build Inventory Service
cd inventory-service
./gradlew clean build
cd ..

# Build Order Service
cd order-service
./gradlew clean build
cd ..
```

### 3. Run the Services

**Terminal 1 - Start Inventory Service:**
```bash
cd inventory-service
./gradlew bootRun
```

**Terminal 2 - Start Order Service:**
```bash
cd order-service
./gradlew bootRun
```

### 4. Verify Services are Running

- Inventory Service: http://localhost:8081
- Order Service: http://localhost:8082
- Inventory H2 Console: http://localhost:8081/h2-console
- Order H2 Console: http://localhost:8082/h2-console

##  API Documentation

### Swagger UI

- **Inventory Service Swagger**: http://localhost:8081/swagger-ui.html
- **Order Service Swagger**: http://localhost:8082/swagger-ui.html

### Inventory Service APIs

#### 1. Get Inventory by Product ID

**Endpoint:** `GET /inventory/{productId}`

**Description:** Returns all inventory batches for a product, sorted by expiry date (FIFO).

**Example Request:**
```bash
curl -X GET http://localhost:8081/inventory/1001
```

**Example Response:**
```json
{
  "productId": 1001,
  "productName": "Laptop",
  "batches": [
    {
      "batchId": 1,
      "quantity": 68,
      "expiryDate": "2026-06-25"
    }
  ]
}
```

#### 2. Update Inventory

**Endpoint:** `POST /inventory/update`

**Description:** Updates inventory after an order is placed (deducts quantity).

**Example Request:**
```bash
curl -X POST http://localhost:8081/inventory/update \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1001,
    "quantity": 5,
    "batchIds": [1]
  }'
```

**Example Response:**
```json
{
  "success": true,
  "message": "Inventory updated successfully",
  "updatedQuantity": 5
}
```

### Order Service APIs

#### 1. Place Order

**Endpoint:** `POST /order`

**Description:** Places a new order, checks inventory availability, and reserves stock.

**Example Request:**
```bash
curl -X POST http://localhost:8082/order \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1002,
    "quantity": 3
  }'
```

**Example Response:**
```json
{
  "orderId": 5012,
  "productId": 1002,
  "productName": "Smartphone",
  "quantity": 3,
  "status": "PLACED",
  "reservedFromBatchIds": [9],
  "message": "Order placed. Inventory reserved."
}
```

##  Database Configuration

Both services use H2 in-memory databases with Liquibase for schema management and data loading.

### H2 Console Access

**Inventory Service:**
- URL: http://localhost:8081/h2-console
- JDBC URL: `jdbc:h2:mem:inventorydb`
- Username: `sa`
- Password: (leave blank)

**Order Service:**
- URL: http://localhost:8082/h2-console
- JDBC URL: `jdbc:h2:mem:orderdb`
- Username: `sa`
- Password: (leave blank)

### Liquibase Changelogs

Data is automatically loaded from CSV files on application startup:

- **Inventory Service**: `src/main/resources/db/data/inventory-data.csv`
- **Order Service**: `src/main/resources/db/data/orders-data.csv`

##  Testing

### Run Unit Tests

```bash
# Inventory Service Tests
cd inventory-service
./gradlew test

# Order Service Tests
cd order-service
./gradlew test
```

### Test Coverage

- **Unit Tests**: Service layer logic with Mockito
- **Controller Tests**: REST endpoint testing with MockMvc
- **Integration Tests**: Full application context with H2 database

### Sample Test Scenarios

1. **Get Inventory** - Verify batches are sorted by expiry date
2. **Place Order** - Successful order placement with inventory reservation
3. **Insufficient Inventory** - Order fails when stock is unavailable
4. **Product Not Found** - Proper error handling for invalid product IDs

##  Factory Design Pattern Implementation

The Inventory Service uses the Factory Pattern to support multiple inventory handling strategies:

### Current Implementations

1. **FIFOInventoryHandler** (Default) - First In, First Out
   - Reserves inventory from batches with earliest expiry dates
   - Minimizes waste by using older stock first

2. **LIFOInventoryHandler** - Last In, First Out
   - Reserves inventory from batches with latest expiry dates
   - Alternative strategy for different business needs

### Switching Strategies

Modify `application.yml` in Inventory Service:

```yaml
inventory:
  handler:
    type: fifo  # Change to 'lifo' for LIFO strategy
```

### Extending with New Strategies

1. Create a new handler implementing `InventoryHandler` interface
2. Annotate with `@Component("customInventoryHandler")`
3. Configure in `application.yml`: `type: custom`

##  Project Structure

```
Testing/
├── inventory-service/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/ecommerce/inventory/
│   │   │   │   ├── controller/        # REST Controllers
│   │   │   │   ├── service/           # Business Logic
│   │   │   │   │   └── impl/          # Handler Implementations
│   │   │   │   ├── factory/           # Factory Pattern
│   │   │   │   ├── repository/        # Data Access Layer
│   │   │   │   ├── entity/            # JPA Entities
│   │   │   │   └── dto/               # Data Transfer Objects
│   │   │   └── resources/
│   │   │       ├── db/
│   │   │       │   ├── changelog/     # Liquibase Changelogs
│   │   │       │   └── data/          # CSV Data Files
│   │   │       └── application.yml
│   │   └── test/                      # Unit & Integration Tests
│   └── pom.xml
│
├── order-service/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/ecommerce/order/
│   │   │   │   ├── controller/        # REST Controllers
│   │   │   │   ├── service/           # Business Logic
│   │   │   │   ├── client/            # External Service Clients
│   │   │   │   ├── repository/        # Data Access Layer
│   │   │   │   ├── entity/            # JPA Entities
│   │   │   │   ├── dto/               # Data Transfer Objects
│   │   │   │   └── config/            # Configuration Classes
│   │   │   └── resources/
│   │   │       ├── db/
│   │   │       │   ├── changelog/     # Liquibase Changelogs
│   │   │       │   └── data/          # CSV Data Files
│   │   │       └── application.yml
│   │   └── test/                      # Unit & Integration Tests
│   └── pom.xml
│
└── README.md
```

##  Application Flow

### Order Placement Flow

1. **Client** sends POST request to Order Service with `productId` and `quantity`
2. **Order Service** calls Inventory Service to check availability
3. **Inventory Service** returns available batches sorted by expiry date
4. **Order Service** validates sufficient inventory exists
5. **Order Service** calculates which batches to reserve (FIFO logic)
6. **Order Service** calls Inventory Service to update/deduct inventory
7. **Inventory Service** deducts quantities from appropriate batches
8. **Order Service** saves order record with reserved batch IDs
9. **Order Service** returns order confirmation to client

### Inventory Reservation Strategy (FIFO)

```
Example: Order 50 units of Product 1001

Available Batches:
- Batch 1: 30 units, expires 2026-01-15
- Batch 2: 40 units, expires 2026-03-20
- Batch 3: 25 units, expires 2026-05-10

Reservation:
- Reserve 30 units from Batch 1 (fully depleted)
- Reserve 20 units from Batch 2 (20 remaining)
- Batch 3 untouched

Result: Order placed with batches [1, 2]
```

##  Technologies Used

- **Spring Boot 3.2.0** - Application framework
- **Spring Data JPA** - Data persistence
- **H2 Database** - In-memory database
- **Liquibase** - Database migration and versioning
- **Lombok** - Boilerplate code reduction
- **SpringDoc OpenAPI** - API documentation
- **RestTemplate** - Synchronous HTTP client for inter-service communication
- **JUnit 5** - Unit testing framework
- **Mockito** - Mocking framework
- **Gradle** - Build and dependency management

##  Sample Data

### Products Available

| Product ID | Product Name | Total Quantity | Batches |
|------------|-------------|----------------|---------|
| 1001 | Laptop | 68 | 1 |
| 1002 | Smartphone | 112 | 2 |
| 1003 | Tablet | 56 | 2 |
| 1004 | Headphones | 76 | 2 |
| 1005 | Smartwatch | 131 | 3 |

##  Troubleshooting

### Issue: Port Already in Use

**Solution:** Change the port in `application.yml`:
```yaml
server:
  port: 8083  # Use different port
```

### Issue: Inventory Service Not Reachable from Order Service

**Solution:** Ensure Inventory Service is running first and verify the URL in Order Service `application.yml`:
```yaml
inventory:
  service:
    url: http://localhost:8081
```

### Issue: Liquibase Migration Fails

**Solution:** Check CSV file format and ensure column names match exactly. Verify file path in changelog XML.

