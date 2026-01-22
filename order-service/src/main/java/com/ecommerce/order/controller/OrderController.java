package com.ecommerce.order.controller;

import com.ecommerce.order.dto.OrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.service.OrderService;
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
 * REST controller for order management operations.
 * Provides endpoints for placing orders and managing order lifecycle.
 */
@Slf4j
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "APIs for managing orders")
public class OrderController {
    
    private final OrderService orderService;
    
    /**
     * Places a new order for a product.
     * Validates inventory availability and reserves inventory from appropriate batches.
     *
     * @param request the order request containing product ID and quantity
     * @return ResponseEntity with OrderResponse containing order details
     */
    @PostMapping
    @Operation(summary = "Place a new order", 
               description = "Creates a new order and reserves inventory from available batches")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Order placed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or insufficient inventory"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<OrderResponse> placeOrder(@RequestBody OrderRequest request) {
        log.info("Received order request: productId={}, quantity={}", 
                request != null ? request.getProductId() : null,
                request != null ? request.getQuantity() : null);
        
        try {
            OrderResponse response = orderService.placeOrder(request);
            log.info("Order placed successfully: orderId={}, productId={}, quantity={}",
                    response.getOrderId(), response.getProductId(), response.getQuantity());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Validation error while placing order: {}", e.getMessage());
            OrderResponse errorResponse = buildErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            
        } catch (RuntimeException e) {
            log.error("Error while placing order: {}", e.getMessage(), e);
            OrderResponse errorResponse = buildErrorResponse(e.getMessage());
            
            // Distinguish between different error types
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            } else if (e.getMessage().contains("Insufficient inventory")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(buildErrorResponse("An error occurred while processing your order"));
            }
        }
    }
    
    /**
     * Builds an error response with the given message.
     *
     * @param message the error message
     * @return OrderResponse with error message
     */
    private OrderResponse buildErrorResponse(String message) {
        OrderResponse errorResponse = new OrderResponse();
        errorResponse.setMessage(message);
        return errorResponse;
    }
}
