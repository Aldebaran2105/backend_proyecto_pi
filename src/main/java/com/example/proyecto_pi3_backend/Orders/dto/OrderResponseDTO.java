package com.example.proyecto_pi3_backend.Orders.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponseDTO {
    private Long id;
    private String status;
    // Epoch milliseconds for pickup time
    private Long pickup_time;
    private Long userId;
    private String userName;
    private Long vendorId;
    private String vendorName;
    private String pickupCode; // Código para recoger el pedido
    private String paymentMethod; // Método de pago: YAPE o PLIN
    private String mercadoPagoPaymentId; // ID del pago en Mercado Pago
    private String mercadoPagoPreferenceId; // ID de la preferencia de pago
    private List<OrderDetailResponseDTO> items;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderDetailResponseDTO {
        private Long id;
        private String itemName;
        private Integer quantity;
        private String price;
        private Long menuItemId; // ID del item del menú para feedback
    }
}

