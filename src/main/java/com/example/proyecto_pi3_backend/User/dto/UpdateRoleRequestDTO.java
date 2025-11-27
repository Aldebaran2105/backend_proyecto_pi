package com.example.proyecto_pi3_backend.User.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateRoleRequestDTO {
    private String role; // "ADMIN", "USER", "VENDOR"
    private Long vendorId; // Opcional: solo necesario si el rol es VENDOR
}

