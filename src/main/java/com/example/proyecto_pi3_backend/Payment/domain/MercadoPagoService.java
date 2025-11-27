package com.example.proyecto_pi3_backend.Payment.domain;

import com.example.proyecto_pi3_backend.Orders.domain.Orders;
import com.example.proyecto_pi3_backend.Orders.infrastructure.OrdersRepository;
import com.example.proyecto_pi3_backend.exception.ResourceNotFoundException;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@Slf4j
@Service
@RequiredArgsConstructor
public class MercadoPagoService {
    
    private final OrdersRepository ordersRepository;
    
    @Value("${mercado-pago.access-token}")
    private String accessToken;
    
    @Value("${mercado-pago.public-key}")
    private String publicKey;
    
    @Value("${app.webhook-url:}")
    private String webhookUrl;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    
    public String generateYapeToken(String phoneNumber, String otp) {
        try {
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                throw new RuntimeException("El número de celular es requerido");
            }
            if (otp == null || otp.trim().isEmpty()) {
                throw new RuntimeException("El código OTP es requerido");
            }
            
            String requestId = UUID.randomUUID().toString();
            java.util.Map<String, String> requestBody = new java.util.HashMap<>();
            requestBody.put("phoneNumber", phoneNumber.trim());
            requestBody.put("otp", otp.trim());
            requestBody.put("requestId", requestId);
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            String url = "https://api.mercadopago.com/platforms/pci/yape/v1/payment?public_key=" + publicKey;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                JsonNode jsonResponse = objectMapper.readTree(response.body());
                if (jsonResponse.has("id")) {
                    return jsonResponse.get("id").asText();
                } else {
                    throw new RuntimeException("Error al generar token Yape: La respuesta no contiene el token");
                }
            } else {
                try {
                    JsonNode errorResponse = objectMapper.readTree(response.body());
                    String errorMessage = errorResponse.has("message") 
                        ? errorResponse.get("message").asText() 
                        : response.body();
                    throw new RuntimeException("Error al generar token Yape: " + errorMessage);
                } catch (Exception e) {
                    throw new RuntimeException("Error al generar token Yape: " + response.body());
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al generar token Yape", e);
            throw new RuntimeException("Error al generar token Yape: " + e.getMessage());
        }
    }
    
    public MercadoPagoPaymentResponse createYapePayment(Long orderId, String token, String payerEmail) {
        try {
            if (token == null || token.trim().isEmpty()) {
                throw new RuntimeException("El token Yape es requerido y no puede estar vacío");
            }
            
            MercadoPagoConfig.setAccessToken(accessToken);
            
            Orders order = ordersRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con ID: " + orderId));
            
            if (order.getOrderDetails() == null || order.getOrderDetails().isEmpty()) {
                throw new RuntimeException("El pedido no tiene items. No se puede procesar el pago.");
            }
            
            BigDecimal total = order.getOrderDetails().stream()
                    .map(detail -> {
                        String priceString = detail.getMenuItem().getPrice();
                        if (priceString == null || priceString.trim().isEmpty()) {
                            throw new RuntimeException("El precio del item '" + detail.getMenuItem().getItemName() + "' no está definido");
                        }
                        
                        priceString = priceString.trim()
                                .replace(",", "")
                                .replace("S/", "")
                                .replace("$", "")
                                .replace(" ", "");
                        
                        if (!priceString.matches("^\\d+(\\.\\d+)?$")) {
                            throw new RuntimeException("El precio del item '" + detail.getMenuItem().getItemName() + 
                                    "' tiene un formato inválido: " + detail.getMenuItem().getPrice());
                        }
                        
                        BigDecimal price;
                        try {
                            price = new BigDecimal(priceString).setScale(2, java.math.RoundingMode.HALF_UP);
                        } catch (NumberFormatException e) {
                            throw new RuntimeException("Error al convertir el precio del item '" + 
                                    detail.getMenuItem().getItemName() + "': " + priceString, e);
                        }
                        
                        if (price.compareTo(BigDecimal.ZERO) <= 0) {
                            throw new RuntimeException("El precio del item '" + detail.getMenuItem().getItemName() + 
                                    "' debe ser mayor a 0");
                        }
                        
                        BigDecimal quantity = BigDecimal.valueOf(detail.getQuantity());
                        BigDecimal subtotal = price.multiply(quantity);
                        return subtotal.setScale(2, java.math.RoundingMode.HALF_UP);
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            total = total.setScale(2, java.math.RoundingMode.HALF_UP);
            
            if (total.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("El total del pedido debe ser mayor a 0");
            }
            
            BigDecimal montoMinimo = new BigDecimal("1.00");
            if (total.compareTo(montoMinimo) < 0) {
                throw new RuntimeException("El monto mínimo para pagos con Yape es S/ 1.00");
            }
            
            BigDecimal montoMaximo = new BigDecimal("999999.99");
            if (total.compareTo(montoMaximo) > 0) {
                throw new RuntimeException("El monto máximo para pagos es S/ 999,999.99");
            }
            
            PaymentClient paymentClient = new PaymentClient();
            
            String montoFormateado = String.format("%.2f", total.doubleValue());
            BigDecimal montoFinal = new BigDecimal(montoFormateado).setScale(2, java.math.RoundingMode.HALF_UP);
            
            if (montoFinal.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("El monto final calculado es inválido: " + montoFinal);
            }
            
            PaymentCreateRequest.PaymentCreateRequestBuilder requestBuilder = PaymentCreateRequest.builder()
                    .transactionAmount(montoFinal)
                    .description("Pedido #" + orderId)
                    .installments(1)
                    .paymentMethodId("yape")
                    .token(token);
            
            if (payerEmail != null && !payerEmail.trim().isEmpty()) {
                String emailLimpio = payerEmail.trim();
                if (emailLimpio.contains("@") && 
                    emailLimpio.length() > 5 &&
                    !emailLimpio.contains("@testuser.com") && 
                    !emailLimpio.contains("@test.com") &&
                    !emailLimpio.contains("@example.com") &&
                    !emailLimpio.contains("test_user") &&
                    !emailLimpio.contains("buyer_")) {
                    requestBuilder.payer(PaymentPayerRequest.builder()
                            .email(emailLimpio)
                            .build());
                }
            }
            
            PaymentCreateRequest paymentRequest = requestBuilder.build();
            Payment payment = paymentClient.create(paymentRequest);
            
            if ("approved".equalsIgnoreCase(payment.getStatus())) {
                order.setStatus("PAGADO");
                order.setMercadoPagoPaymentId(String.valueOf(payment.getId()));
            } else {
                String mensajeError = "El pago fue rechazado por Mercado Pago";
                if (payment.getStatusDetail() != null) {
                    switch (payment.getStatusDetail()) {
                        case "cc_rejected_other_reason":
                            mensajeError = "El pago fue rechazado. Verifica que el token Yape sea válido y que estés usando datos reales con credenciales de producción.";
                            break;
                        case "cc_rejected_insufficient_amount":
                            mensajeError = "El pago fue rechazado por fondos insuficientes.";
                            break;
                        case "cc_rejected_bad_filled_security_code":
                            mensajeError = "El código OTP de Yape es incorrecto.";
                            break;
                        default:
                            mensajeError = "El pago fue rechazado por Mercado Pago: " + payment.getStatusDetail();
                    }
                }
                throw new RuntimeException(mensajeError);
            }
            
            ordersRepository.save(order);
            
            MercadoPagoPaymentResponse response = new MercadoPagoPaymentResponse();
            response.setPreferenceId(String.valueOf(payment.getId()));
            response.setTotal(total);
            response.setPaymentMethod("YAPE");
            
            return response;
            
        } catch (MPApiException e) {
            String errorDetails = "Error desconocido";
            
            if (e.getApiResponse() != null && e.getApiResponse().getContent() != null) {
                String content = e.getApiResponse().getContent();
                try {
                    JsonNode errorJson = objectMapper.readTree(content);
                    
                    if (errorJson.has("message")) {
                        errorDetails = errorJson.get("message").asText();
                    } else if (errorJson.has("error")) {
                        errorDetails = errorJson.get("error").asText();
                    } else if (errorJson.has("cause")) {
                        JsonNode causeArray = errorJson.get("cause");
                        if (causeArray.isArray() && causeArray.size() > 0) {
                            JsonNode firstCause = causeArray.get(0);
                            if (firstCause.has("description")) {
                                errorDetails = firstCause.get("description").asText();
                            } else if (firstCause.has("message")) {
                                errorDetails = firstCause.get("message").asText();
                            }
                        }
                    } else {
                        errorDetails = content;
                    }
                } catch (Exception parseException) {
                    errorDetails = content;
                }
            } else if (e.getMessage() != null) {
                errorDetails = e.getMessage();
            }
            
            if (errorDetails.contains("Invalid users involved") || e.getMessage().contains("Invalid users involved")) {
                errorDetails = "Invalid users involved: El token Yape puede haber expirado o no ser válido.";
            }
            
            throw new RuntimeException("Error al crear pago Yape en Mercado Pago: " + errorDetails);
        } catch (MPException e) {
            log.error("Error general de Mercado Pago al crear pago Yape", e);
            throw new RuntimeException("Error al crear pago Yape: " + e.getMessage());
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al crear pago Yape", e);
            throw new RuntimeException("Error inesperado al crear pago Yape: " + e.getMessage());
        }
    }
    
    public void processWebhook(String paymentId, String preferenceId) {
        try {
            Orders order = null;
            
            if (paymentId != null && !paymentId.trim().isEmpty()) {
                try {
                    order = ordersRepository.findByMercadoPagoPaymentId(paymentId).orElse(null);
                } catch (Exception e) {
                    log.warn("No se pudo buscar pedido por paymentId: {}", e.getMessage());
                }
            }
            
            if (order == null && preferenceId != null && !preferenceId.trim().isEmpty()) {
                try {
                    order = ordersRepository.findByMercadoPagoPreferenceId(preferenceId).orElse(null);
                } catch (Exception e) {
                    log.warn("No se pudo buscar pedido por preferenceId: {}", e.getMessage());
                }
            }
            
            if (order == null) {
                log.warn("No se encontró pedido con paymentId: {} o preferenceId: {}", paymentId, preferenceId);
                return;
            }
            
            MercadoPagoConfig.setAccessToken(accessToken);
            PaymentClient paymentClient = new PaymentClient();
            Payment payment = paymentClient.get(Long.parseLong(paymentId));
            
            if (payment != null) {
                String status = payment.getStatus();
                
                if ("approved".equalsIgnoreCase(status)) {
                    if (!"PAGADO".equals(order.getStatus())) {
                        order.setStatus("PAGADO");
                        order.setMercadoPagoPaymentId(paymentId);
                        ordersRepository.save(order);
                    }
                } else if ("rejected".equalsIgnoreCase(status) || "cancelled".equalsIgnoreCase(status)) {
                    if (!"CANCELADO".equals(order.getStatus())) {
                        order.setStatus("CANCELADO");
                        ordersRepository.save(order);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error al procesar webhook de Mercado Pago", e);
        }
    }
}
