package com.ecommerce.order.service;

import com.ecommerce.order.client.InventoryClient;
import com.ecommerce.order.dto.*;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service class for handling order operations.
 * Manages order placement, inventory validation, and reservation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    
    private static final String BATCH_ID_DELIMITER = ",";
    
    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    
    /**
     * Places a new order for a product.
     * Validates inventory availability, reserves inventory, and persists the order.
     *
     * @param request the order request containing product ID and quantity
     * @return OrderResponse containing order details and confirmation
     * @throws IllegalArgumentException if request is null or invalid
     * @throws RuntimeException if product not found or insufficient inventory
     */
    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        validateOrderRequest(request);
        
        log.info("Processing order for product ID: {}, quantity: {}", 
                request.getProductId(), request.getQuantity());
        
        InventoryResponse inventoryResponse = inventoryClient.getInventory(request.getProductId());
        
        if (inventoryResponse == null) {
            log.error("Product not found with ID: {}", request.getProductId());
            throw new RuntimeException("Product not found with ID: " + request.getProductId());
        }
        
        validateInventoryAvailability(inventoryResponse, request.getQuantity());
        
        List<Long> reservedBatchIds = reserveInventoryBatches(
                inventoryResponse.getBatches(), 
                request.getQuantity()
        );
        
        updateInventory(request.getProductId(), request.getQuantity(), reservedBatchIds);
        
        Order order = createOrder(request, inventoryResponse, reservedBatchIds);
        Order savedOrder = orderRepository.save(order);
        
        log.info("Order placed successfully. Order ID: {}, Product ID: {}, Quantity: {}",
                savedOrder.getOrderId(), savedOrder.getProductId(), savedOrder.getQuantity());
        
        return buildOrderResponse(savedOrder, reservedBatchIds);
    }
    
    /**
     * Validates the order request parameters.
     *
     * @param request the order request to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateOrderRequest(OrderRequest request) {
        if (request == null) {
            log.error("Order request cannot be null");
            throw new IllegalArgumentException("Order request cannot be null");
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
    
    /**
     * Validates that sufficient inventory is available for the requested quantity.
     *
     * @param inventoryResponse the inventory response
     * @param requestedQuantity the requested quantity
     * @throws RuntimeException if insufficient inventory
     */
    private void validateInventoryAvailability(InventoryResponse inventoryResponse, Integer requestedQuantity) {
        int totalAvailable = inventoryResponse.getBatches().stream()
                .mapToInt(BatchDTO::getQuantity)
                .sum();
        
        if (totalAvailable < requestedQuantity) {
            log.error("Insufficient inventory. Available: {}, Requested: {}", 
                    totalAvailable, requestedQuantity);
            throw new RuntimeException(String.format(
                    "Insufficient inventory. Available: %d, Requested: %d", 
                    totalAvailable, requestedQuantity));
        }
        
        log.debug("Inventory validation passed. Available: {}, Requested: {}", 
                totalAvailable, requestedQuantity);
    }
    
    /**
     * Reserves inventory batches for the order.
     *
     * @param batches list of available batches
     * @param requestedQuantity quantity to reserve
     * @return list of batch IDs from which inventory was reserved
     */
    private List<Long> reserveInventoryBatches(List<BatchDTO> batches, int requestedQuantity) {
        List<Long> reservedBatchIds = new ArrayList<>();
        int remainingQuantity = requestedQuantity;
        
        for (BatchDTO batch : batches) {
            if (remainingQuantity <= 0) {
                break;
            }
            
            if (batch.getQuantity() > 0) {
                reservedBatchIds.add(batch.getBatchId());
                int reservedFromBatch = Math.min(batch.getQuantity(), remainingQuantity);
                remainingQuantity -= reservedFromBatch;
                
                log.debug("Reserved {} units from batch ID: {}", reservedFromBatch, batch.getBatchId());
            }
        }
        
        return reservedBatchIds;
    }
    
    /**
     * Updates inventory by calling the Inventory Service.
     *
     * @param productId the product ID
     * @param quantity the quantity to deduct
     * @param reservedBatchIds the batch IDs to update
     * @throws RuntimeException if inventory update fails
     */
    private void updateInventory(Long productId, Integer quantity, List<Long> reservedBatchIds) {
        InventoryUpdateRequest updateRequest = new InventoryUpdateRequest(
                productId,
                quantity,
                reservedBatchIds
        );
        
        log.debug("Updating inventory for product ID: {}", productId);
        InventoryUpdateResponse updateResponse = inventoryClient.updateInventory(updateRequest);
        
        if (!updateResponse.isSuccess()) {
            log.error("Failed to update inventory: {}", updateResponse.getMessage());
            throw new RuntimeException("Failed to update inventory: " + updateResponse.getMessage());
        }
        
        log.debug("Inventory updated successfully for product ID: {}", productId);
    }
    
    /**
     * Creates an Order entity from the request and inventory response.
     *
     * @param request the order request
     * @param inventoryResponse the inventory response
     * @param reservedBatchIds the reserved batch IDs
     * @return Order entity
     */
    private Order createOrder(OrderRequest request, InventoryResponse inventoryResponse, 
                             List<Long> reservedBatchIds) {
        Order order = new Order();
        order.setProductId(request.getProductId());
        order.setProductName(inventoryResponse.getProductName());
        order.setQuantity(request.getQuantity());
        order.setStatus(OrderStatus.PLACED);
        order.setOrderDate(LocalDate.now());
        order.setReservedBatchIds(formatBatchIds(reservedBatchIds));
        return order;
    }
    
    /**
     * Formats batch IDs into a comma-separated string.
     *
     * @param batchIds list of batch IDs
     * @return comma-separated string of batch IDs
     */
    private String formatBatchIds(List<Long> batchIds) {
        return batchIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(BATCH_ID_DELIMITER));
    }
    
    /**
     * Builds the order response from the saved order.
     *
     * @param savedOrder the saved order entity
     * @param reservedBatchIds the reserved batch IDs
     * @return OrderResponse
     */
    private OrderResponse buildOrderResponse(Order savedOrder, List<Long> reservedBatchIds) {
        return new OrderResponse(
                savedOrder.getOrderId(),
                savedOrder.getProductId(),
                savedOrder.getProductName(),
                savedOrder.getQuantity(),
                savedOrder.getStatus().name(),
                reservedBatchIds,
                "Order placed successfully. Inventory reserved."
        );
    }
}
