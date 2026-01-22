package com.ecommerce.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryUpdateRequest {
    private Long productId;
    private Integer quantity;
    private List<Long> batchIds;
}
