package com.example.proyecto_pi3_backend.MenuItems.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MenuItemsRequestDTO {
    private String itemName;
    private String description;
    private String price;
    private Long vendorId;
    private Integer stock;
    private Boolean isAvailable;
    private String date; // Fecha en formato yyyy-MM-dd (opcional, si no se proporciona usa la fecha actual)
}
