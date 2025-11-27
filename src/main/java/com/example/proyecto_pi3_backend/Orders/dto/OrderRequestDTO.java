package com.example.proyecto_pi3_backend.Orders.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequestDTO {
    private Long userId;
    private Long vendorId;
    private String paymentMethod; // YAPE o PLIN
    private List<OrderItemDTO> items;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderItemDTO {
        private Long menuItemId;
        private Integer quantity;
    }
}

