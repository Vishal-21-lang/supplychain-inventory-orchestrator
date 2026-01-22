package com.ecommerce.inventory.service.impl;

import com.ecommerce.inventory.dto.BatchDTO;
import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.entity.InventoryBatch;
import com.ecommerce.inventory.repository.InventoryBatchRepository;
import com.ecommerce.inventory.service.InventoryHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * LIFO (Last-In-First-Out) implementation of inventory handler.
 * Reserves inventory from batches with the latest expiry dates first.
 */
@Slf4j
@Component("lifoInventoryHandler")
@RequiredArgsConstructor
public class LIFOInventoryHandler implements InventoryHandler {
    
    private static final int MINIMUM_QUANTITY = 0;
    
    private final InventoryBatchRepository inventoryBatchRepository;
    
    /**
     * Retrieves inventory information for a given product.
     *
     * @param productId the ID of the product
     * @return InventoryResponse containing product details and available batches
     * @throws IllegalArgumentException if productId is null
     */
    @Override
    public InventoryResponse getInventoryByProductId(Long productId) {
        if (productId == null) {
            log.error("Product ID cannot be null");
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        
        log.debug("Fetching inventory for product ID: {}", productId);
        List<InventoryBatch> batches = inventoryBatchRepository
                .findByProductIdOrderByExpiryDateAsc(productId);
        
        if (batches.isEmpty()) {
            log.warn("No inventory found for product ID: {}", productId);
            return null;
        }
        
        List<BatchDTO> batchDTOs = batches.stream()
                .map(this::convertToBatchDTO)
                .collect(Collectors.toList());
        
        log.info("Found {} batches for product ID: {}", batchDTOs.size(), productId);
        return new InventoryResponse(
                productId,
                batches.get(0).getProductName(),
                batchDTOs
        );
    }
    
    /**
     * Reserves inventory using LIFO strategy (latest expiry dates first).
     *
     * @param productId the ID of the product
     * @param quantity the quantity to reserve
     * @param reservedBatchIds list to store IDs of batches from which inventory was reserved
     * @return true if the full quantity was successfully reserved, false otherwise
     * @throws IllegalArgumentException if any parameter is null or quantity is negative
     */
    @Override
    public boolean reserveInventory(Long productId, Integer quantity, List<Long> reservedBatchIds) {
        validateReserveInventoryParams(productId, quantity, reservedBatchIds);
        
        log.debug("Reserving {} units for product ID: {} using LIFO strategy", quantity, productId);
        List<InventoryBatch> batches = inventoryBatchRepository
                .findByProductIdAndQuantityGreaterThanOrderByExpiryDateAsc(productId, MINIMUM_QUANTITY);
        
        // Reverse for LIFO (latest expiry first)
        Collections.reverse(batches);
        
        int remainingQuantity = quantity;
        
        for (InventoryBatch batch : batches) {
            if (remainingQuantity <= 0) {
                break;
            }
            
            int deductQuantity = Math.min(batch.getQuantity(), remainingQuantity);
            batch.setQuantity(batch.getQuantity() - deductQuantity);
            inventoryBatchRepository.save(batch);
            
            reservedBatchIds.add(batch.getBatchId());
            remainingQuantity -= deductQuantity;
            
            log.debug("Reserved {} units from batch ID: {}", deductQuantity, batch.getBatchId());
        }
        
        boolean success = remainingQuantity == 0;
        if (success) {
            log.info("Successfully reserved {} units for product ID: {}", quantity, productId);
        } else {
            log.warn("Could not reserve full quantity. Remaining: {} units for product ID: {}", 
                    remainingQuantity, productId);
        }
        
        return success;
    }
    
    /**
     * Finds all available batches for a given product.
     *
     * @param productId the ID of the product
     * @return list of available inventory batches
     * @throws IllegalArgumentException if productId is null
     */
    @Override
    public List<InventoryBatch> findAvailableBatches(Long productId) {
        if (productId == null) {
            log.error("Product ID cannot be null");
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        
        log.debug("Finding available batches for product ID: {}", productId);
        return inventoryBatchRepository
                .findByProductIdAndQuantityGreaterThanOrderByExpiryDateAsc(productId, MINIMUM_QUANTITY);
    }
    
    /**
     * Converts InventoryBatch entity to BatchDTO.
     *
     * @param batch the inventory batch entity
     * @return BatchDTO representation
     */
    private BatchDTO convertToBatchDTO(InventoryBatch batch) {
        return new BatchDTO(
                batch.getBatchId(),
                batch.getQuantity(),
                batch.getExpiryDate()
        );
    }
    
    /**
     * Validates parameters for reserveInventory method.
     *
     * @param productId the product ID
     * @param quantity the quantity to reserve
     * @param reservedBatchIds the list to store reserved batch IDs
     * @throws IllegalArgumentException if validation fails
     */
    private void validateReserveInventoryParams(Long productId, Integer quantity, List<Long> reservedBatchIds) {
        if (productId == null) {
            log.error("Product ID cannot be null");
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        if (quantity == null || quantity <= 0) {
            log.error("Quantity must be positive. Received: {}", quantity);
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (reservedBatchIds == null) {
            log.error("Reserved batch IDs list cannot be null");
            throw new IllegalArgumentException("Reserved batch IDs list cannot be null");
        }
    }
}
