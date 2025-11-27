package com.example.proyecto_pi3_backend;

import com.example.proyecto_pi3_backend.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFoundException(ResourceNotFoundException ex){
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<Map<String, String>> handleAuthenticationException(AuthenticationException ex){
        Map<String, String> error = new HashMap<>();
        error.put("message", "Credenciales inválidas. Verifica tu email y contraseña.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex){
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolationException(DataIntegrityViolationException ex){
        Map<String, String> error = new HashMap<>();
        String message = ex.getMessage();
        log.error("DataIntegrityViolationException", ex);
        
        if (message != null && message.contains("email")) {
            error.put("message", "El email ya está registrado");
        } else if (message != null && message.contains("role_check")) {
            error.put("message", "El rol proporcionado no es válido. Los roles válidos son: ADMIN, USER, VENDOR");
        } else {
            error.put("message", "Error de integridad de datos: " + (message != null ? message : ex.getClass().getSimpleName()));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex){
        Map<String, String> error = new HashMap<>();
        String message = "Método HTTP no soportado: " + ex.getMethod() + ". Métodos permitidos: " + 
                        (ex.getSupportedHttpMethods() != null ? ex.getSupportedHttpMethods().toString() : "N/A");
        log.error("HttpRequestMethodNotSupportedException: {}", message, ex);
        error.put("message", message);
        error.put("errorType", ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex){
        Map<String, String> error = new HashMap<>();
        log.error("Error inesperado", ex);
        error.put("message", "Ocurrió un error inesperado: " + ex.getMessage());
        error.put("errorType", ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
