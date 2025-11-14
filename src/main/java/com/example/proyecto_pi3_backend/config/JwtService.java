package com.example.proyecto_pi3_backend.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.proyecto_pi3_backend.User.domain.UserService;
import com.example.proyecto_pi3_backend.User.domain.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JwtService {
    @Value("${jwt-secret}")
    private String secret;

    private final UserService userService;

    public JwtService(UserService userService){
        this.userService = userService;
    }
    public String generateToken(UserDetails userDetails){
        Users user = (Users) userDetails;
        Date now = new Date();
        Date expiration = new Date(now.getTime() + 1000 *60 *60 *10);
        Algorithm algorithm = Algorithm.HMAC256(secret);

        String roleName = user.getRole() != null ? user.getRole().name() : "USER";

        return JWT.create()
                .withSubject(user.getId().toString())
                .withClaim("email", user.getEmail())
                .withClaim("role", roleName)
                .withIssuedAt(now)
                .withExpiresAt(expiration)
                .sign(algorithm);
    }

    public boolean validateToken(String token){
        try {
            JWT.require(Algorithm.HMAC256(secret)).build().verify(token);
            return true;
        } catch (Exception e){
            return false;
        }
    }

    public Long extractUserId(String token) {
        return Long.parseLong(JWT.decode(token).getSubject());
    }
}
