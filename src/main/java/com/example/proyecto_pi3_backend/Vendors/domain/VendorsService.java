package com.example.proyecto_pi3_backend.Vendors.domain;

import com.example.proyecto_pi3_backend.Vendors.dto.VendorRequestDTO;
import com.example.proyecto_pi3_backend.Vendors.dto.VendorResponseDTO;
import com.example.proyecto_pi3_backend.Vendors.infrastructure.VendorsRepository;
import com.example.proyecto_pi3_backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de vendors (puestos de comida)
 * Lógica de negocio para operaciones con vendors
 */
@Service
@RequiredArgsConstructor
public class VendorsService {
    private final VendorsRepository vendorsRepository;

    /**
     * Obtiene todos los vendors
     * @return Lista de todos los vendors
     */
    public List<VendorResponseDTO> getAllVendors() {
        return vendorsRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene un vendor por ID
     * @param id ID del vendor
     * @return DTO del vendor
     * @throws ResourceNotFoundException si el vendor no existe
     */
    public VendorResponseDTO getVendorById(Long id) {
        Vendors vendor = vendorsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor no encontrado con ID: " + id));
        
        return mapToDTO(vendor);
    }

    /**
     * Crea un nuevo vendor
     * @param requestDTO Datos del vendor a crear
     * @return DTO del vendor creado
     */
    @Transactional
    public VendorResponseDTO createVendor(VendorRequestDTO requestDTO) {
        // Validar datos
        if (requestDTO.getName() == null || requestDTO.getName().trim().isEmpty()) {
            throw new RuntimeException("El nombre del vendor es requerido");
        }
        
        // Crear entidad
        Vendors vendor = new Vendors();
        vendor.setName(requestDTO.getName().trim());
        vendor.setUbication(requestDTO.getUbication() != null ? requestDTO.getUbication().trim() : null);
        
        // Convertir horarios de String a LocalTime
        vendor.setOpeningTime(parseTime(requestDTO.getOpeningTime()));
        vendor.setClosingTime(parseTime(requestDTO.getClosingTime()));
        
        // Validar que el horario de cierre sea después del de apertura
        if (vendor.getOpeningTime() != null && vendor.getClosingTime() != null) {
            if (vendor.getClosingTime().isBefore(vendor.getOpeningTime()) || vendor.getClosingTime().equals(vendor.getOpeningTime())) {
                throw new RuntimeException("La hora de cierre debe ser posterior a la hora de apertura");
            }
        }
        
        // Guardar
        vendor = vendorsRepository.save(vendor);
        
        return mapToDTO(vendor);
    }

    /**
     * Actualiza un vendor existente
     * @param id ID del vendor a actualizar
     * @param requestDTO Datos actualizados del vendor
     * @return DTO del vendor actualizado
     * @throws ResourceNotFoundException si el vendor no existe
     */
    @Transactional
    public VendorResponseDTO updateVendor(Long id, VendorRequestDTO requestDTO) {
        // Validar datos
        if (requestDTO.getName() == null || requestDTO.getName().trim().isEmpty()) {
            throw new RuntimeException("El nombre del vendor es requerido");
        }
        
        // Buscar vendor
        Vendors vendor = vendorsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor no encontrado con ID: " + id));
        
        // Actualizar datos
        vendor.setName(requestDTO.getName().trim());
        vendor.setUbication(requestDTO.getUbication() != null ? requestDTO.getUbication().trim() : null);
        
        // Convertir horarios de String a LocalTime
        if (requestDTO.getOpeningTime() != null && !requestDTO.getOpeningTime().trim().isEmpty()) {
            vendor.setOpeningTime(parseTime(requestDTO.getOpeningTime()));
        }
        if (requestDTO.getClosingTime() != null && !requestDTO.getClosingTime().trim().isEmpty()) {
            vendor.setClosingTime(parseTime(requestDTO.getClosingTime()));
        }
        
        // Validar que el horario de cierre sea después del de apertura
        if (vendor.getOpeningTime() != null && vendor.getClosingTime() != null) {
            if (vendor.getClosingTime().isBefore(vendor.getOpeningTime()) || vendor.getClosingTime().equals(vendor.getOpeningTime())) {
                throw new RuntimeException("La hora de cierre debe ser posterior a la hora de apertura");
            }
        }
        
        // Guardar
        vendor = vendorsRepository.save(vendor);
        
        return mapToDTO(vendor);
    }

    /**
     * Elimina un vendor
     * @param id ID del vendor a eliminar
     * @throws ResourceNotFoundException si el vendor no existe
     */
    @Transactional
    public void deleteVendor(Long id) {
        // Verificar que existe
        Vendors vendor = vendorsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor no encontrado con ID: " + id));
        
        // Eliminar
        vendorsRepository.delete(vendor);
    }

    /**
     * Mapea una entidad Vendors a VendorResponseDTO
     */
    private VendorResponseDTO mapToDTO(Vendors vendor) {
        return new VendorResponseDTO(
                vendor.getId(),
                vendor.getName(),
                vendor.getUbication(),
                formatTime(vendor.getOpeningTime()),
                formatTime(vendor.getClosingTime())
        );
    }
    
    /**
     * Convierte un String en formato "HH:mm" a LocalTime
     * @param timeString String en formato "HH:mm" o null
     * @return LocalTime o null si el string es null o vacío
     * @throws RuntimeException si el formato es inválido
     */
    private LocalTime parseTime(String timeString) {
        if (timeString == null || timeString.trim().isEmpty()) {
            return null;
        }
        
        try {
            return LocalTime.parse(timeString.trim(), DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException e) {
            throw new RuntimeException("Formato de hora inválido. Use el formato HH:mm (ej: 08:00, 17:30)");
        }
    }
    
    /**
     * Convierte un LocalTime a String en formato "HH:mm"
     * @param time LocalTime o null
     * @return String en formato "HH:mm" o null
     */
    private String formatTime(LocalTime time) {
        if (time == null) {
            return null;
        }
        return time.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
