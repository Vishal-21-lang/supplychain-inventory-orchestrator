package com.ecommerce.order.controller;

import com.ecommerce.order.dto.OrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private OrderService orderService;
    
    @Test
    void testPlaceOrder_Success() throws Exception {
        OrderRequest request = new OrderRequest(1002L, 3);
        OrderResponse response = new OrderResponse(
                5012L, 1002L, "Smartphone", 3, "PLACED",
                Arrays.asList(9L), "Order placed. Inventory reserved."
        );
        
        when(orderService.placeOrder(any(OrderRequest.class))).thenReturn(response);
        
        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(5012))
                .andExpect(jsonPath("$.productId").value(1002))
                .andExpect(jsonPath("$.productName").value("Smartphone"))
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.message").value("Order placed. Inventory reserved."));
    }
    
    @Test
    void testPlaceOrder_Failure() throws Exception {
        OrderRequest request = new OrderRequest(9999L, 5);
        
        when(orderService.placeOrder(any(OrderRequest.class)))
                .thenThrow(new RuntimeException("Product not found"));
        
        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Product not found"));
    }
}
