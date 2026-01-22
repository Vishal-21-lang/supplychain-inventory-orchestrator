package com.ecommerce.inventory.factory;

import com.ecommerce.inventory.service.InventoryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class InventoryHandlerFactory {
    
    private final Map<String, InventoryHandler> inventoryHandlers;
    
    public InventoryHandler getHandler(String handlerType) {
        InventoryHandler handler = inventoryHandlers.get(handlerType + "InventoryHandler");
        
        if (handler == null) {
            throw new IllegalArgumentException("Unknown handler type: " + handlerType);
        }
        
        return handler;
    }
}
