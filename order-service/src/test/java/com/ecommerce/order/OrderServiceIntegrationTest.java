package com.ecommerce.order;

import com.ecommerce.order.entity.Order;
import com.ecommerce.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OrderServiceIntegrationTest {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Test
    void testOrderDataLoadedFromCSV() {
        long count = orderRepository.count();
        assertTrue(count >= 10, "Expected at least 10 orders to be loaded");
        
        Order order = orderRepository.findById(1L).orElse(null);
        assertNotNull(order);
        assertEquals(1005L, order.getProductId());
        assertEquals("Smartwatch", order.getProductName());
        assertEquals(10, order.getQuantity());
    }
}
