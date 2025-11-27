package com.example.proyecto_pi3_backend.MenuItems.domain;

import com.example.proyecto_pi3_backend.Availability.domain.Availability;
import com.example.proyecto_pi3_backend.Feedback.domain.Feedback;
import com.example.proyecto_pi3_backend.OrderDetails.domain.OrderDetails;
import com.example.proyecto_pi3_backend.Vendors.domain.Vendors;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
public class MenuItems {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String itemName;

    private String description;

    private String price;

    @OneToMany(mappedBy = "menuItem", cascade = {}, orphanRemoval = false)
    private List<Feedback> feedback = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendors vendor;

    @OneToMany(mappedBy = "menuItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderDetails> orderDetails = new ArrayList<>();

    @OneToMany(mappedBy = "menuItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Availability> availabilityHistory;
}
