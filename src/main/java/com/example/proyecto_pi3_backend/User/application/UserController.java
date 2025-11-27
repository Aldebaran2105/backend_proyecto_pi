package com.example.proyecto_pi3_backend.User.application;

import com.example.proyecto_pi3_backend.User.domain.UserService;
import com.example.proyecto_pi3_backend.User.dto.UpdateRoleRequestDTO;
import com.example.proyecto_pi3_backend.User.dto.UserResponseDTO;
import com.example.proyecto_pi3_backend.config.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final JwtService jwtService;

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
    
    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            String token = authHeader.substring(7);
            Long userId = jwtService.extractUserId(token);
            return ResponseEntity.ok(userService.getUserById(userId));
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<UserResponseDTO> updateUserRole(
            @PathVariable Long id,
            @RequestBody UpdateRoleRequestDTO request) {
        
        if (request == null) {
            throw new RuntimeException("El request body no puede ser null");
        }
        
        if (request.getRole() == null || request.getRole().trim().isEmpty()) {
            throw new RuntimeException("El rol es requerido");
        }
        
        try {
            UserResponseDTO result = userService.updateUserRole(id, request.getRole(), request.getVendorId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error al actualizar rol", e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }
}
