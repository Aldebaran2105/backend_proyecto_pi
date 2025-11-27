package com.example.proyecto_pi3_backend.Payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YapePaymentRequest {
    private String token; // Token generado por el SDK JS de Mercado Pago
    private String payerEmail; // Email del pagador
    private String phoneNumber; // Número de celular (opcional, para logging)
}

