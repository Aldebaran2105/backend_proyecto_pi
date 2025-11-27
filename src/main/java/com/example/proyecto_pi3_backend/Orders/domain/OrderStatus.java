package com.example.proyecto_pi3_backend.Orders.domain;

public enum OrderStatus {
    PENDIENTE_PAGO,
    PENDIENTE_VERIFICACION,
    PAGADO,
    LISTO_PARA_RECOJO,
    COMPLETADO,
    CANCELADO
}