package com.example.proyecto_pi3_backend.Payment.application;

import com.example.proyecto_pi3_backend.Payment.domain.MercadoPagoPaymentResponse;
import com.example.proyecto_pi3_backend.Payment.domain.MercadoPagoService;
import com.example.proyecto_pi3_backend.Payment.dto.YapePaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {
    
    private final MercadoPagoService mercadoPagoService;
    
    @PostMapping("/yape/token")
    public ResponseEntity<?> generateYapeToken(
            @RequestParam String phoneNumber,
            @RequestParam String otp) {
        try {
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("El número de celular es requerido");
            }
            if (otp == null || otp.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("El código OTP es requerido");
            }
            return ResponseEntity.ok(mercadoPagoService.generateYapeToken(phoneNumber.trim(), otp.trim()));
        } catch (Exception e) {
            log.error("Error al generar token Yape", e);
            return ResponseEntity.status(500).body("Error al generar token: " + e.getMessage());
        }
    }
    
    @PostMapping("/yape/{orderId}")
    public ResponseEntity<?> createYapePayment(
            @PathVariable Long orderId,
            @RequestBody YapePaymentRequest request) {
        try {
            if (request == null) {
                return ResponseEntity.badRequest().body("El request es requerido");
            }
            
            if (request.getToken() == null || request.getToken().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("El token es requerido");
            }
            if (request.getPayerEmail() == null || request.getPayerEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("El email del pagador es requerido");
            }
            if (orderId == null || orderId <= 0) {
                return ResponseEntity.badRequest().body("El ID del pedido es inválido");
            }
            
            MercadoPagoPaymentResponse response = mercadoPagoService.createYapePayment(
                    orderId, 
                    request.getToken().trim(), 
                    request.getPayerEmail().trim()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al crear pago Yape", e);
            
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "Error desconocido al crear pago Yape";
            }
            
            Map<String, String> error = new HashMap<>();
            error.put("message", errorMessage);
            error.put("errorType", e.getClass().getSimpleName());
            
            return ResponseEntity.status(400).body(error);
        }
    }
    
    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(
            @RequestParam(required = false) String data_id,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String preference_id,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            String paymentId = null;
            String preferenceId = null;
            
            if (body != null) {
                if (body.containsKey("data")) {
                    Object dataObj = body.get("data");
                    if (dataObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = (Map<String, Object>) dataObj;
                        paymentId = (String) data.get("id");
                    }
                }
                if (body.containsKey("data_id")) {
                    paymentId = (String) body.get("data_id");
                }
                if (body.containsKey("preference_id")) {
                    preferenceId = (String) body.get("preference_id");
                }
                if (body.containsKey("type")) {
                    type = (String) body.get("type");
                }
            }
            
            if (paymentId == null && data_id != null) {
                paymentId = data_id;
            }
            if (preferenceId == null && preference_id != null) {
                preferenceId = preference_id;
            }
            
            if (paymentId != null && ("payment".equals(type) || type == null)) {
                mercadoPagoService.processWebhook(paymentId, preferenceId);
            }
            
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.ok("OK");
        }
    }
}
