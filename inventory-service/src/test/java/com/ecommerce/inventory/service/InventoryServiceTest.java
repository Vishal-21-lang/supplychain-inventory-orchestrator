package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.dto.InventoryUpdateRequest;
import com.ecommerce.inventory.dto.InventoryUpdateResponse;
import com.ecommerce.inventory.entity.InventoryBatch;
import com.ecommerce.inventory.factory.InventoryHandlerFactory;
import com.ecommerce.inventory.service.impl.FIFOInventoryHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {
    
    @Mock
    private InventoryHandlerFactory handlerFactory;
    
    @Mock
    private FIFOInventoryHandler fifoHandler;
    
    @InjectMocks
    private InventoryService inventoryService;
    
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(inventoryService, "handlerType", "fifo");
    }
    
    @Test
    void testGetInventoryByProductId_Success() {
        Long productId = 1001L;
        InventoryResponse expectedResponse = new InventoryResponse();
        expectedResponse.setProductId(productId);
        expectedResponse.setProductName("Laptop");
        
        when(handlerFactory.getHandler("fifo")).thenReturn(fifoHandler);
        when(fifoHandler.getInventoryByProductId(productId)).thenReturn(expectedResponse);
        
        InventoryResponse result = inventoryService.getInventoryByProductId(productId);
        
        assertNotNull(result);
        assertEquals(productId, result.getProductId());
        assertEquals("Laptop", result.getProductName());
        verify(handlerFactory).getHandler("fifo");
        verify(fifoHandler).getInventoryByProductId(productId);
    }
    
    @Test
    void testGetInventoryByProductId_NotFound() {
        Long productId = 9999L;
        
        when(handlerFactory.getHandler("fifo")).thenReturn(fifoHandler);
        when(fifoHandler.getInventoryByProductId(productId)).thenReturn(null);
        
        assertThrows(RuntimeException.class, () -> {
            inventoryService.getInventoryByProductId(productId);
        });
    }
    
    @Test
    void testUpdateInventory_Success() {
        InventoryUpdateRequest request = new InventoryUpdateRequest(1001L, 10, new ArrayList<>());
        
        when(handlerFactory.getHandler("fifo")).thenReturn(fifoHandler);
        when(fifoHandler.reserveInventory(eq(1001L), eq(10), anyList())).thenReturn(true);
        
        InventoryUpdateResponse response = inventoryService.updateInventory(request);
        
        assertTrue(response.isSuccess());
        assertEquals("Inventory updated successfully", response.getMessage());
        assertEquals(10, response.getUpdatedQuantity());
    }
    
    @Test
    void testUpdateInventory_InsufficientInventory() {
        InventoryUpdateRequest request = new InventoryUpdateRequest(1001L, 100, new ArrayList<>());
        
        when(handlerFactory.getHandler("fifo")).thenReturn(fifoHandler);
        when(fifoHandler.reserveInventory(eq(1001L), eq(100), anyList())).thenReturn(false);
        
        assertThrows(RuntimeException.class, () -> {
            inventoryService.updateInventory(request);
        });
    }
}
