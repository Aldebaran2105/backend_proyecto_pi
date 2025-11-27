package com.example.proyecto_pi3_backend.Orders.domain;

import com.example.proyecto_pi3_backend.Feedback.domain.Feedback;
import com.example.proyecto_pi3_backend.OrderDetails.domain.OrderDetails;
import com.example.proyecto_pi3_backend.User.domain.Users;
import com.example.proyecto_pi3_backend.Vendors.domain.Vendors;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
public class Orders {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Timestamp pickup_time;

    private Timestamp createdAt; // Fecha y hora de creación del pedido (para calcular tiempo de pago)

    private String status;

    private String pickupCode; // Código único para recoger el pedido
    
    private String paymentMethod; // Método de pago: YAPE o PLIN
    
    private String mercadoPagoPaymentId; // ID del pago en Mercado Pago
    private String mercadoPagoPreferenceId; // ID de la preferencia de pago en Mercado Pago

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private Users user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Feedback> feedback = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendors vendor;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<OrderDetails> orderDetails = new ArrayList<>();
}
