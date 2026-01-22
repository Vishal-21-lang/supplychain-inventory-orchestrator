package com.ecommerce.inventory.controller;

import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.dto.InventoryUpdateRequest;
import com.ecommerce.inventory.dto.InventoryUpdateResponse;
import com.ecommerce.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for inventory management operations.
 * Provides endpoints for retrieving and updating inventory batches.
 */
@Slf4j
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory Management", description = "APIs for managing inventory batches")
public class InventoryController {
    
    private final InventoryService inventoryService;
    
    /**
     * Retrieves inventory information for a specific product.
     * Returns all available batches sorted by expiry date.
     *
     * @param productId the ID of the product
     * @return ResponseEntity with InventoryResponse containing batch details
     */
    @GetMapping("/{productId}")
    @Operation(summary = "Get inventory batches by product ID", 
               description = "Returns all inventory batches for a product sorted by expiry date")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Inventory retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid product ID"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<InventoryResponse> getInventory(@PathVariable Long productId) {
        log.info("Received request to get inventory for product ID: {}", productId);
        
        try {
            InventoryResponse response = inventoryService.getInventoryByProductId(productId);
            log.info("Successfully retrieved inventory for product ID: {}. Total batches: {}",
                    productId, response.getBatches().size());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Validation error for product ID {}: {}", productId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            
        } catch (RuntimeException e) {
            log.error("Error retrieving inventory for product ID {}: {}", productId, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Updates inventory by reserving the specified quantity.
     * Deducts inventory from available batches using configured strategy (FIFO/LIFO).
     *
     * @param request the inventory update request containing product ID, quantity, and batch IDs
     * @return ResponseEntity with InventoryUpdateResponse indicating success or failure
     */
    @PostMapping("/update")
    @Operation(summary = "Update inventory after order", 
               description = "Deducts inventory quantity after an order is placed using FIFO/LIFO strategy")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Inventory updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or insufficient inventory"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<InventoryUpdateResponse> updateInventory(
            @RequestBody InventoryUpdateRequest request) {
        log.info("Received request to update inventory: productId={}, quantity={}",
                request != null ? request.getProductId() : null,
                request != null ? request.getQuantity() : null);
        
        try {
            InventoryUpdateResponse response = inventoryService.updateInventory(request);
            log.info("Successfully updated inventory for product ID: {}. Quantity deducted: {}",
                    request.getProductId(), request.getQuantity());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Validation error while updating inventory: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new InventoryUpdateResponse(false, e.getMessage(), 0));
            
        } catch (RuntimeException e) {
            log.error("Error updating inventory for product ID {}: {}",
                    request != null ? request.getProductId() : null, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new InventoryUpdateResponse(false, e.getMessage(), 0));
        }
    }
}
