package com.ecommerce.inventory.service;

import com.ecommerce.inventory.dto.InventoryResponse;
import com.ecommerce.inventory.entity.InventoryBatch;

import java.util.List;

public interface InventoryHandler {
    InventoryResponse getInventoryByProductId(Long productId);
    boolean reserveInventory(Long productId, Integer quantity, List<Long> reservedBatchIds);
    List<InventoryBatch> findAvailableBatches(Long productId);
}
