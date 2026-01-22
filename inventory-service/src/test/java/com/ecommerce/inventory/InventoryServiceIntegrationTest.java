package com.ecommerce.inventory;

import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.entity.InventoryBatch;
import com.ecommerce.inventory.repository.InventoryBatchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InventoryServiceIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private InventoryBatchRepository inventoryBatchRepository;
    
    @Test
    void testGetInventoryEndpoint() {
        String url = "http://localhost:" + port + "/inventory/1001";
        
        ResponseEntity<InventoryResponse> response = restTemplate.getForEntity(url, InventoryResponse.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1001L, response.getBody().getProductId());
        assertEquals("Laptop", response.getBody().getProductName());
        assertFalse(response.getBody().getBatches().isEmpty());
    }
    
    @Test
    void testInventoryDataLoadedFromCSV() {
        long count = inventoryBatchRepository.count();
        assertTrue(count >= 10, "Expected at least 10 inventory batches to be loaded");
        
        InventoryBatch batch = inventoryBatchRepository.findById(1L).orElse(null);
        assertNotNull(batch);
        assertEquals(1001L, batch.getProductId());
        assertEquals("Laptop", batch.getProductName());
    }
}
