package com.example.proyecto_pi3_backend.Auth.domain;

import com.example.proyecto_pi3_backend.Auth.dto.AuthResponse;
import com.example.proyecto_pi3_backend.Auth.dto.LoginRequest;
import com.example.proyecto_pi3_backend.Auth.dto.RegisterRequest;
import com.example.proyecto_pi3_backend.User.domain.Role;
import com.example.proyecto_pi3_backend.User.domain.Users;
import com.example.proyecto_pi3_backend.User.infrastructure.UserRepository;
import com.example.proyecto_pi3_backend.config.JwtService;
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
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new RuntimeException("El email es requerido");
        }
        
        if (userRepository.findByEmail(request.getEmail().trim().toLowerCase()).isPresent()) {
            throw new RuntimeException("El email ya está registrado");
        }

        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new RuntimeException("El nombre es requerido");
        }
        
        if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            throw new RuntimeException("El apellido es requerido");
        }
        
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new RuntimeException("La contraseña es requerida");
        }

        String email = request.getEmail().trim().toLowerCase();
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        
        try {
            userRepository.insertUserWithRoleNative(
                request.getFirstName().trim(),
                request.getLastName().trim(),
                email,
                encodedPassword,
                "USER"
            );
        } catch (Exception e) {
            try {
                Users user = new Users();
                user.setFirstName(request.getFirstName().trim());
                user.setLastName(request.getLastName().trim());
                user.setEmail(email);
                user.setPassword(encodedPassword);
                user.setRole(Role.USER);
                userRepository.save(user);
            } catch (Exception e2) {
                throw new RuntimeException("Error al guardar el usuario: " + e2.getMessage());
            }
        }
        
        Users user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Error al crear usuario"));

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

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Users user = (Users) userDetails;

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
