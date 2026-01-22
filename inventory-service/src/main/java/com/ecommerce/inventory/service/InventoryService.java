package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.dto.InventoryUpdateRequest;
import com.ecommerce.inventory.dto.InventoryUpdateResponse;
import com.ecommerce.inventory.factory.InventoryHandlerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

/**
 * Service class for managing inventory operations.
 * Uses Factory Pattern to delegate operations to appropriate handlers (FIFO/LIFO).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {
    
    private final InventoryHandlerFactory handlerFactory;
    
    @Value("${inventory.handler.type:fifo}")
    private String handlerType;
    
    /**
     * Retrieves inventory information for a given product.
     *
     * @param productId the ID of the product
     * @return InventoryResponse containing product details and available batches
     * @throws IllegalArgumentException if productId is null
     * @throws RuntimeException if product not found
     */
    public InventoryResponse getInventoryByProductId(Long productId) {
        if (productId == null) {
            log.error("Product ID cannot be null");
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        
        log.debug("Fetching inventory for product ID: {} using handler: {}", productId, handlerType);
        InventoryHandler handler = handlerFactory.getHandler(handlerType);
        InventoryResponse response = handler.getInventoryByProductId(productId);
        
        if (response == null) {
            log.error("Product not found with ID: {}", productId);
            throw new RuntimeException("Product not found with ID: " + productId);
        }
        
        log.info("Successfully retrieved inventory for product ID: {}", productId);
        return response;
    }
    
    /**
     * Updates inventory by reserving the specified quantity.
     * This operation is transactional - either all inventory is reserved or none.
     *
     * @param request the inventory update request
     * @return InventoryUpdateResponse indicating success or failure
     * @throws IllegalArgumentException if request is null or invalid
     * @throws RuntimeException if insufficient inventory
     */
    @Transactional
    public InventoryUpdateResponse updateInventory(InventoryUpdateRequest request) {
        validateUpdateRequest(request);
        
        log.info("Updating inventory for product ID: {}, quantity: {} using handler: {}",
                request.getProductId(), request.getQuantity(), handlerType);
        
        InventoryHandler handler = handlerFactory.getHandler(handlerType);
        
        ArrayList<Long> reservedBatchIds = new ArrayList<>();
        boolean success = handler.reserveInventory(
                request.getProductId(),
                request.getQuantity(),
                reservedBatchIds
        );
        
        if (success) {
            log.info("Inventory updated successfully for product ID: {}. Reserved batches: {}",
                    request.getProductId(), reservedBatchIds);
            return new InventoryUpdateResponse(
                    true,
                    "Inventory updated successfully",
                    request.getQuantity()
            );
        } else {
            log.error("Insufficient inventory for product ID: {}", request.getProductId());
            throw new RuntimeException("Insufficient inventory for product ID: " + request.getProductId());
        }
    }
    
    /**
     * Validates the inventory update request.
     *
     * @param request the request to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateUpdateRequest(InventoryUpdateRequest request) {
        if (request == null) {
            log.error("Inventory update request cannot be null");
            throw new IllegalArgumentException("Inventory update request cannot be null");
        }
        if (request.getProductId() == null) {
            log.error("Product ID cannot be null");
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            log.error("Quantity must be positive. Received: {}", request.getQuantity());
            throw new IllegalArgumentException("Quantity must be positive");
        }
    }
}
