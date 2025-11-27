package com.example.proyecto_pi3_backend.config;

import com.example.proyecto_pi3_backend.User.domain.Role;
import com.example.proyecto_pi3_backend.User.domain.Users;
import com.example.proyecto_pi3_backend.User.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        try {
            Users user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));
            
            // Si el rol es null o hay problemas con el enum, establecer USER por defecto
            if (user.getRole() == null) {
                try {
                    // Intentar corregir el rol en la BD usando query nativa
                    userRepository.updateUserRoleNative(user.getId(), "USER");
                    // Recargar el usuario
                    user = userRepository.findByEmail(email)
                            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));
                } catch (Exception e) {
                    // Si falla, establecer USER por defecto en memoria
                    user.setRole(Role.USER);
                }
            }
            
            return user;
        } catch (Exception e) {
            // Si hay error al cargar (posiblemente por tipo de dato en role), intentar con query nativa
            throw new UsernameNotFoundException("Error al cargar usuario: " + e.getMessage());
        }
    }
}

