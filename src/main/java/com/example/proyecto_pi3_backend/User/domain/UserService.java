package com.example.proyecto_pi3_backend.User.domain;

import com.example.proyecto_pi3_backend.User.dto.UserResponseDTO;
import com.example.proyecto_pi3_backend.User.infrastructure.UserRepository;
import com.example.proyecto_pi3_backend.Vendors.infrastructure.VendorsRepository;
import com.example.proyecto_pi3_backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final VendorsRepository vendorsRepository;

    public List<UserResponseDTO> getAllUsers() {
        try {
            List<Object[]> rawUsers = userRepository.findAllUsersRaw();
            List<UserResponseDTO> result = new ArrayList<>();
            
            for (Object[] row : rawUsers) {
                try {
                    Long id = ((Number) row[0]).longValue();
                    String firstName = (String) row[1];
                    String lastName = (String) row[2];
                    String email = (String) row[3];
                    Object roleObj = row[4];
                    Long vendorId = row[5] != null ? ((Number) row[5]).longValue() : null;
                    
                    String roleStr = roleObj != null ? roleObj.toString().trim() : null;
                    String roleName = "USER";
                    
                    if (roleStr != null && !roleStr.isEmpty()) {
                        String roleUpper = roleStr.toUpperCase().trim();
                        if (roleUpper.equals("ADMIN") || roleUpper.equals("USER") || roleUpper.equals("VENDOR")) {
                            roleName = roleUpper;
                        }
                    }
                    
                    result.add(new UserResponseDTO(id, firstName, lastName, email, roleName, vendorId));
                } catch (Exception e) {
                    log.warn("Error procesando usuario: {}", e.getMessage());
                }
            }
            
            return result;
        } catch (Exception e) {
            log.error("Error al obtener usuarios", e);
            throw new RuntimeException("Error al obtener usuarios: " + e.getMessage());
        }
    }

    public UserResponseDTO getUserById(Long id) {
        try {
            Users user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
            
            return mapToDTO(user);
        } catch (Exception e) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + id);
        }
    }

    @Transactional
    public UserResponseDTO updateUserRole(Long userId, String roleStr, Long vendorId) {
        String normalizedRole = validateAndNormalizeRole(roleStr);
        Role newRole = Role.valueOf(normalizedRole);
        
        Users user;
        try {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));
        } catch (Exception e) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + userId);
        }
        
        try {
            userRepository.updateUserRoleNative(userId, normalizedRole);
        } catch (Exception e) {
            try {
                userRepository.updateUserRoleNativeAsInt(userId, normalizedRole);
            } catch (Exception e2) {
                try {
                    user.setRole(newRole);
                    userRepository.save(user);
                } catch (Exception e3) {
                    log.error("Error al actualizar rol", e3);
                    throw new RuntimeException("No se pudo actualizar el rol. La columna 'role' debe ser VARCHAR. Error: " + e3.getMessage());
                }
            }
        }
        
        if (normalizedRole.equals("VENDOR")) {
            if (vendorId != null && vendorId > 0) {
                vendorsRepository.findById(vendorId)
                        .orElseThrow(() -> new ResourceNotFoundException("Vendor no encontrado con ID: " + vendorId));
                userRepository.updateUserVendorNative(userId, vendorId);
            } else {
                userRepository.clearUserVendorNative(userId);
            }
        } else {
            userRepository.clearUserVendorNative(userId);
        }
        
        user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));
        
        return mapToDTO(user);
    }

    private String validateAndNormalizeRole(String roleStr) {
        if (roleStr == null || roleStr.trim().isEmpty()) {
            throw new RuntimeException("El rol es requerido");
        }
        
        String normalized = roleStr.toUpperCase().trim();
        
        if (!normalized.equals("ADMIN") && !normalized.equals("USER") && !normalized.equals("VENDOR")) {
            throw new RuntimeException("Rol inválido: " + roleStr + ". Los roles válidos son: ADMIN, USER, VENDOR");
        }
        
        return normalized;
    }

    private UserResponseDTO mapToDTO(Users user) {
        String roleName = "USER";
        try {
            if (user.getRole() != null) {
                roleName = user.getRole().name();
            }
        } catch (Exception e) {
            roleName = "USER";
        }
        
        Long vendorId = null;
        try {
            if (user.getVendor() != null) {
                vendorId = user.getVendor().getId();
            }
        } catch (Exception e) {
            vendorId = null;
        }
        
        return new UserResponseDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                roleName,
                vendorId
        );
    }
}
