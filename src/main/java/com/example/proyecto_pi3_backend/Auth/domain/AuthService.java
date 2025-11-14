package com.example.proyecto_pi3_backend.Auth.domain;

import com.example.proyecto_pi3_backend.Auth.dto.AuthResponse;
import com.example.proyecto_pi3_backend.Auth.dto.LoginRequest;
import com.example.proyecto_pi3_backend.Auth.dto.RegisterRequest;
import com.example.proyecto_pi3_backend.User.domain.Role;
import com.example.proyecto_pi3_backend.User.domain.Users;
import com.example.proyecto_pi3_backend.User.infrastructure.UserRepository;
import com.example.proyecto_pi3_backend.config.JwtService;
import com.example.proyecto_pi3_backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Validar que el email no esté vacío
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new RuntimeException("El email es requerido");
        }
        
        // Validar que el email no esté ya registrado
        if (userRepository.findAll().stream()
                .anyMatch(user -> user.getEmail() != null && user.getEmail().equals(request.getEmail()))) {
            throw new RuntimeException("El email ya está registrado");
        }

        // Validar campos requeridos
        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new RuntimeException("El nombre es requerido");
        }
        
        if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            throw new RuntimeException("El apellido es requerido");
        }
        
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new RuntimeException("La contraseña es requerida");
        }

        Users user = new Users();
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        
        // Siempre asignar rol USER por defecto al registrarse
        user.setRole(Role.USER);

        try {
            user = userRepository.save(user);
        } catch (Exception e) {
            throw new RuntimeException("Error al guardar el usuario: " + e.getMessage());
        }

        String token = jwtService.generateToken(user);

        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole() != null ? user.getRole().name() : "USER",
                token
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Users user = userRepository.findAll().stream()
                .filter(u -> u.getEmail().equals(userDetails.getUsername()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        String token = jwtService.generateToken(user);

        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole() != null ? user.getRole().name() : "USER",
                token
        );
    }
}
