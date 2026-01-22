package com.ecommerce.order.service;

import com.ecommerce.order.client.InventoryClient;
import com.ecommerce.order.dto.*;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private InventoryClient inventoryClient;
    
    @InjectMocks
    private OrderService orderService;
    
    @Test
    void testPlaceOrder_Success() {
        OrderRequest request = new OrderRequest(1002L, 3);
        
        List<BatchDTO> batches = Arrays.asList(
                new BatchDTO(9L, 29, LocalDate.of(2026, 5, 31)),
                new BatchDTO(10L, 83, LocalDate.of(2026, 11, 15))
        );
        InventoryResponse inventoryResponse = new InventoryResponse(1002L, "Smartphone", batches);
        
        InventoryUpdateResponse updateResponse = new InventoryUpdateResponse(true, "Updated", 3);
        
        Order savedOrder = new Order();
        savedOrder.setOrderId(5012L);
        savedOrder.setProductId(1002L);
        savedOrder.setProductName("Smartphone");
        savedOrder.setQuantity(3);
        savedOrder.setStatus(OrderStatus.PLACED);
        savedOrder.setOrderDate(LocalDate.now());
        
        when(inventoryClient.getInventory(1002L)).thenReturn(inventoryResponse);
        when(inventoryClient.updateInventory(any(InventoryUpdateRequest.class))).thenReturn(updateResponse);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        
        OrderResponse response = orderService.placeOrder(request);
        
        assertNotNull(response);
        assertEquals(5012L, response.getOrderId());
        assertEquals(1002L, response.getProductId());
        assertEquals("Smartphone", response.getProductName());
        assertEquals(3, response.getQuantity());
        assertEquals("PLACED", response.getStatus());
        assertEquals("Order placed. Inventory reserved.", response.getMessage());
        
        verify(inventoryClient).getInventory(1002L);
        verify(inventoryClient).updateInventory(any(InventoryUpdateRequest.class));
        verify(orderRepository).save(any(Order.class));
    }
    
    @Test
    void testPlaceOrder_ProductNotFound() {
        OrderRequest request = new OrderRequest(9999L, 5);
        
        when(inventoryClient.getInventory(9999L)).thenReturn(null);
        
        assertThrows(RuntimeException.class, () -> {
            orderService.placeOrder(request);
        });
    }
    
    @Test
    void testPlaceOrder_InsufficientInventory() {
        OrderRequest request = new OrderRequest(1002L, 200);
        
        List<BatchDTO> batches = Arrays.asList(
                new BatchDTO(9L, 29, LocalDate.of(2026, 5, 31)),
                new BatchDTO(10L, 83, LocalDate.of(2026, 11, 15))
        );
        InventoryResponse inventoryResponse = new InventoryResponse(1002L, "Smartphone", batches);
        
        when(inventoryClient.getInventory(1002L)).thenReturn(inventoryResponse);
        
        assertThrows(RuntimeException.class, () -> {
            orderService.placeOrder(request);
        });
    }
}
