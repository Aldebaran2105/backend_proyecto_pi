package com.example.proyecto_pi3_backend.Vendors.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VendorRequestDTO {
    private String name;
    private String ubication;
    private String openingTime; // Hora de apertura (formato HH:mm, ej: "08:00")
    private String closingTime; // Hora de cierre (formato HH:mm, ej: "17:00")
}

