package com.example.proyecto_pi3_backend.MenuItems.application;

import com.example.proyecto_pi3_backend.MenuItems.domain.MenuItemsService;
import com.example.proyecto_pi3_backend.MenuItems.dto.MenuItemsRequestDTO;
import com.example.proyecto_pi3_backend.MenuItems.dto.MenuItemsResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para gestión de menús
 * 
 * Endpoints para ADMIN:
 * - POST /menu-items - Crear menú
 * - PUT /menu-items/{id} - Actualizar menú
 * - DELETE /menu-items/{id} - Eliminar menú
 * - GET /menu-items - Ver todos los menús
 * 
 * Endpoints para USUARIO:
 * - GET /menu-items/today - Menús del día
 * - GET /menu-items/vendor/{vendorId}/today - Menús del día por vendor
 * - GET /menu-items/date/{date} - Menús por fecha
 * - GET /menu-items/vendor/{vendorId}/date/{date} - Menús por vendor y fecha
 * - GET /menu-items/week/{weekStartDate} - Menús de la semana
 * - GET /menu-items/vendor/{vendorId}/week/{weekStartDate} - Menús de la semana por vendor
 */
@RestController
@RequestMapping("/menu-items")
@RequiredArgsConstructor
public class MenuItemsController {
    private final MenuItemsService menuItemsService;

    /**
     * ADMIN: Obtiene todos los menús (para gestión)
     */
    @GetMapping
    public ResponseEntity<List<MenuItemsResponseDTO>> getAllMenuItems() {
        return ResponseEntity.ok(menuItemsService.getAllMenuItems());
    }

    /**
     * VENDOR: Obtiene todos los menús de un vendor con todas sus disponibilidades
     */
    @GetMapping("/vendor/{vendorId}/all")
    public ResponseEntity<List<MenuItemsResponseDTO>> getAllMenuItemsByVendor(@PathVariable Long vendorId) {
        return ResponseEntity.ok(menuItemsService.getAllMenuItemsByVendor(vendorId));
    }

    /**
     * ADMIN: Crea un nuevo menú
     */
    @PostMapping
    public ResponseEntity<MenuItemsResponseDTO> createMenuItem(@RequestBody MenuItemsRequestDTO requestDTO) {
        return ResponseEntity.ok(menuItemsService.createMenuItem(requestDTO));
    }

    /**
     * ADMIN: Actualiza un menú
     */
    @PutMapping("/{id}")
    public ResponseEntity<MenuItemsResponseDTO> updateMenuItem(
            @PathVariable Long id,
            @RequestBody MenuItemsRequestDTO requestDTO) {
        return ResponseEntity.ok(menuItemsService.updateMenuItem(id, requestDTO));
    }

    /**
     * ADMIN: Elimina un menú
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable Long id) {
        menuItemsService.deleteMenuItem(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * VENDOR: Elimina una disponibilidad específica (fecha) de un menú
     */
    @DeleteMapping("/{id}/availability")
    public ResponseEntity<Void> deleteMenuItemAvailability(
            @PathVariable Long id,
            @RequestParam String date) {
        menuItemsService.deleteMenuItemAvailability(id, date);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtiene un menú por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<MenuItemsResponseDTO> getMenuItemById(@PathVariable Long id) {
        return ResponseEntity.ok(menuItemsService.getMenuItemById(id));
    }

    /**
     * USUARIO: Obtiene menús disponibles del día (hoy)
     */
    @GetMapping("/today")
    public ResponseEntity<List<MenuItemsResponseDTO>> getAvailableMenuItemsToday() {
        return ResponseEntity.ok(menuItemsService.getAvailableMenuItemsToday());
    }

    /**
     * USUARIO: Obtiene menús disponibles por vendor del día
     */
    @GetMapping("/vendor/{vendorId}/today")
    public ResponseEntity<List<MenuItemsResponseDTO>> getAvailableMenuItemsByVendorToday(@PathVariable Long vendorId) {
        return ResponseEntity.ok(menuItemsService.getAvailableMenuItemsByVendorToday(vendorId));
    }

    /**
     * USUARIO: Obtiene menús disponibles por fecha
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<List<MenuItemsResponseDTO>> getAvailableMenuItemsByDate(@PathVariable String date) {
        return ResponseEntity.ok(menuItemsService.getAvailableMenuItemsByDate(date));
    }

    /**
     * USUARIO: Obtiene menús disponibles por vendor y fecha
     */
    @GetMapping("/vendor/{vendorId}/date/{date}")
    public ResponseEntity<List<MenuItemsResponseDTO>> getAvailableMenuItemsByVendorAndDate(
            @PathVariable Long vendorId,
            @PathVariable String date) {
        return ResponseEntity.ok(menuItemsService.getAvailableMenuItemsByVendorAndDate(vendorId, date));
    }

    /**
     * USUARIO: Obtiene menús disponibles de la semana
     */
    @GetMapping("/week/{weekStartDate}")
    public ResponseEntity<List<MenuItemsResponseDTO>> getAvailableMenuItemsByWeek(@PathVariable String weekStartDate) {
        return ResponseEntity.ok(menuItemsService.getAvailableMenuItemsByWeek(weekStartDate));
    }

    /**
     * USUARIO: Obtiene menús disponibles por vendor y semana
     */
    @GetMapping("/vendor/{vendorId}/week/{weekStartDate}")
    public ResponseEntity<List<MenuItemsResponseDTO>> getAvailableMenuItemsByVendorAndWeek(
            @PathVariable Long vendorId,
            @PathVariable String weekStartDate) {
        return ResponseEntity.ok(menuItemsService.getAvailableMenuItemsByVendorAndWeek(vendorId, weekStartDate));
    }
}
