package com.example.proyecto_pi3_backend.Payment.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MercadoPagoPaymentResponse {
    private String preferenceId;
    private BigDecimal total;
    private String paymentMethod;
}

