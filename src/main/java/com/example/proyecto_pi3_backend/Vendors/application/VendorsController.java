package com.example.proyecto_pi3_backend.Vendors.application;

import com.example.proyecto_pi3_backend.Vendors.domain.VendorsService;
import com.example.proyecto_pi3_backend.Vendors.dto.VendorRequestDTO;
import com.example.proyecto_pi3_backend.Vendors.dto.VendorResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para gestión de vendors
 * Solo maneja HTTP, delega lógica al servicio
 */
@RestController
@RequestMapping("/vendors")
@RequiredArgsConstructor
public class VendorsController {
    private final VendorsService vendorsService;

    @GetMapping
    public ResponseEntity<List<VendorResponseDTO>> getAllVendors() {
        return ResponseEntity.ok(vendorsService.getAllVendors());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VendorResponseDTO> getVendorById(@PathVariable Long id) {
        return ResponseEntity.ok(vendorsService.getVendorById(id));
    }

    @PostMapping
    public ResponseEntity<VendorResponseDTO> createVendor(@RequestBody VendorRequestDTO requestDTO) {
        return ResponseEntity.ok(vendorsService.createVendor(requestDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VendorResponseDTO> updateVendor(
            @PathVariable Long id,
            @RequestBody VendorRequestDTO requestDTO) {
        return ResponseEntity.ok(vendorsService.updateVendor(id, requestDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVendor(@PathVariable Long id) {
        vendorsService.deleteVendor(id);
        return ResponseEntity.noContent().build();
    }
}
