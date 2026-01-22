# Quick Start Guide

##  Get Running in 5 Minutes

### Step 1: Build Both Services (2 minutes)

Open two terminal windows in the `/Users/visharma/Desktop/SCP/Testing` directory.

**Terminal 1 - Build Inventory Service:**
```bash
cd inventory-service
./gradlew clean build
```

**Terminal 2 - Build Order Service:**
```bash
cd order-service
./gradlew clean build
```

### Step 2: Start Services (1 minute)

**Terminal 1 - Start Inventory Service:**
```bash
./gradlew bootRun
```

Wait for: `Started InventoryServiceApplication in X seconds`

**Terminal 2 - Start Order Service:**
```bash
./gradlew bootRun
```

Wait for: `Started OrderServiceApplication in X seconds`

### Step 3: Test the APIs (2 minutes)

#### Test 1: Get Inventory
```bash
curl http://localhost:8081/inventory/1002
```

**Expected Response:**
```json
{
  "productId": 1002,
  "productName": "Smartphone",
  "batches": [
    {
      "batchId": 9,
      "quantity": 29,
      "expiryDate": "2026-05-31"
    },
    {
      "batchId": 10,
      "quantity": 83,
      "expiryDate": "2026-11-15"
    }
  ]
}
```

#### Test 2: Place an Order
```bash
curl -X POST http://localhost:8082/order \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1002,
    "quantity": 3
  }'
```

**Expected Response:**
```json
{
  "orderId": 11,
  "productId": 1002,
  "productName": "Smartphone",
  "quantity": 3,
  "status": "PLACED",
  "reservedFromBatchIds": [9],
  "message": "Order placed. Inventory reserved."
}
```

#### Test 3: Verify Inventory Updated
```bash
curl http://localhost:8081/inventory/1002
```

**Expected Response** (notice quantity reduced from 29 to 26):
```json
{
  "productId": 1002,
  "productName": "Smartphone",
  "batches": [
    {
      "batchId": 9,
      "quantity": 26,
      "expiryDate": "2026-05-31"
    },
    {
      "batchId": 10,
      "quantity": 83,
      "expiryDate": "2026-11-15"
    }
  ]
}
```

##  Quick Access URLs

- **Inventory Service Swagger**: http://localhost:8081/swagger-ui.html
- **Order Service Swagger**: http://localhost:8082/swagger-ui.html
- **Inventory H2 Console**: http://localhost:8081/h2-console
- **Order H2 Console**: http://localhost:8082/h2-console

##  Run Tests

```bash
# Test Inventory Service
cd inventory-service
./gradlew test

# Test Order Service
cd order-service
./gradlew test
```

##  Available Products

| Product ID | Name | Total Quantity | Batches |
|------------|------|----------------|---------|
| 1001 | Laptop | 68 | 1 |
| 1002 | Smartphone | 112 | 2 |
| 1003 | Tablet | 56 | 2 |
| 1004 | Headphones | 76 | 2 |
| 1005 | Smartwatch | 131 | 3 |

##  Understanding the Flow

1. **Order Service** receives order request
2. **Order Service** calls **Inventory Service** to check availability
3. **Inventory Service** returns batches sorted by expiry date (FIFO)
4. **Order Service** validates sufficient inventory exists
5. **Order Service** calls **Inventory Service** to reserve/deduct inventory
6. **Inventory Service** updates batch quantities
7. **Order Service** saves order record
8. **Order Service** returns confirmation to client

##  Next Steps

- Read `README.md` for complete documentation
- Read `ARCHITECTURE_FLOW.md` for detailed flow explanations
- Explore Swagger UI for interactive API testing
- Check H2 Console to see database changes in real-time
- Run tests to understand the test coverage

##  Key Learning Points

### 1. Factory Design Pattern
Located in: `inventory-service/src/main/java/com/ecommerce/inventory/factory/`

The factory pattern allows switching between FIFO and LIFO strategies:
```yaml
# In application.yml
inventory:
  handler:
    type: fifo  # Change to 'lifo' to switch strategy
```

### 2. Microservices Communication
Located in: `order-service/src/main/java/com/ecommerce/order/client/InventoryClient.java`

Uses WebClient for non-blocking REST calls between services.

### 3. Liquibase Migrations
Located in: `*/src/main/resources/db/changelog/`

Automatically creates tables and loads CSV data on startup.

### 4. Layered Architecture
```
Controller → Service → Repository → Database
```

Each layer has a specific responsibility and can be tested independently.

##  Troubleshooting

### Port Already in Use
```bash
# Find process using port 8081 or 8082
lsof -i :8081
kill -9 <PID>
```

### Build Fails
```bash
# Clean and rebuild
./gradlew clean build --refresh-dependencies
```

### Services Can't Communicate
Ensure Inventory Service (8081) starts before Order Service (8082).

##  Try These Scenarios

### Scenario 1: Insufficient Inventory
```bash
curl -X POST http://localhost:8082/order \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1002,
    "quantity": 200
  }'
```
Expected: Error message about insufficient inventory

### Scenario 2: Product Not Found
```bash
curl http://localhost:8081/inventory/9999
```
Expected: 404 Not Found

### Scenario 3: Multiple Orders (FIFO Logic)
```bash
# Order 1: Uses batch with earliest expiry
curl -X POST http://localhost:8082/order \
  -H "Content-Type: application/json" \
  -d '{"productId": 1005, "quantity": 10}'

# Order 2: Continues from same batch or next batch
curl -X POST http://localhost:8082/order \
  -H "Content-Type: application/json" \
  -d '{"productId": 1005, "quantity": 30}'

# Check inventory to see FIFO in action
curl http://localhost:8081/inventory/1005
```


