package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.dto.BatchDTO;
import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.dto.InventoryUpdateRequest;
import com.ecommerce.inventory.dto.InventoryUpdateResponse;
import com.ecommerce.inventory.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private InventoryService inventoryService;
    
    @Test
    void testGetInventory_Success() throws Exception {
        Long productId = 1001L;
        List<BatchDTO> batches = Arrays.asList(
                new BatchDTO(1L, 50, LocalDate.of(2025, 12, 31)),
                new BatchDTO(2L, 30, LocalDate.of(2026, 3, 15))
        );
        InventoryResponse response = new InventoryResponse(productId, "Laptop", batches);
        
        when(inventoryService.getInventoryByProductId(productId)).thenReturn(response);
        
        mockMvc.perform(get("/inventory/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.productName").value("Laptop"))
                .andExpect(jsonPath("$.batches.length()").value(2));
    }
    
    @Test
    void testGetInventory_NotFound() throws Exception {
        Long productId = 9999L;
        
        when(inventoryService.getInventoryByProductId(productId))
                .thenThrow(new RuntimeException("Product not found"));
        
        mockMvc.perform(get("/inventory/{productId}", productId))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void testUpdateInventory_Success() throws Exception {
        InventoryUpdateRequest request = new InventoryUpdateRequest(1001L, 10, Arrays.asList(1L));
        InventoryUpdateResponse response = new InventoryUpdateResponse(true, "Inventory updated successfully", 10);
        
        when(inventoryService.updateInventory(any(InventoryUpdateRequest.class))).thenReturn(response);
        
        mockMvc.perform(post("/inventory/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Inventory updated successfully"));
    }
}
