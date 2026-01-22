package com.ecommerce.order.client;

import com.ecommerce.order.dto.InventoryResponse;
import com.ecommerce.order.dto.InventoryUpdateRequest;
import com.ecommerce.order.dto.InventoryUpdateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class InventoryClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;
    
    public InventoryResponse getInventory(Long productId) {
        String url = inventoryServiceUrl + "/inventory/" + productId;
        return restTemplate.getForObject(url, InventoryResponse.class);
    }
    
    public InventoryUpdateResponse updateInventory(InventoryUpdateRequest request) {
        String url = inventoryServiceUrl + "/inventory/update";
        return restTemplate.postForObject(url, request, InventoryUpdateResponse.class);
    }
}
