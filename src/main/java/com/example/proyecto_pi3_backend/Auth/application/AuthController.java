package com.example.proyecto_pi3_backend.Auth.application;

import com.example.proyecto_pi3_backend.Auth.domain.AuthService;
import com.example.proyecto_pi3_backend.Auth.dto.AuthResponse;
import com.example.proyecto_pi3_backend.Auth.dto.LoginRequest;
import com.example.proyecto_pi3_backend.Auth.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}

