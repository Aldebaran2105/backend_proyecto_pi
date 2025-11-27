package com.example.proyecto_pi3_backend.Orders.application;

import com.example.proyecto_pi3_backend.Orders.domain.OrdersService;
import com.example.proyecto_pi3_backend.Orders.dto.OrderRequestDTO;
import com.example.proyecto_pi3_backend.Orders.dto.OrderResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrdersController {
    private final OrdersService ordersService;

    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(@RequestBody OrderRequestDTO orderRequestDTO) {
        return ResponseEntity.ok(ordersService.createOrder(orderRequestDTO));
    }

    @PostMapping("/{orderId}/pay")
    public ResponseEntity<OrderResponseDTO> payOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(ordersService.payOrder(orderId));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable Long orderId) {
        return ResponseEntity.ok(ordersService.getOrderById(orderId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponseDTO>> getOrdersByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(ordersService.getOrdersByUserId(userId));
    }

    @GetMapping("/vendor/{vendorId}")
    public ResponseEntity<List<OrderResponseDTO>> getOrdersByVendorId(
            @PathVariable Long vendorId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // Validar que el usuario autenticado tiene acceso a este vendor
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                Long userId = ordersService.extractUserIdFromToken(token);
                // Verificar que el usuario es VENDOR y tiene acceso a este vendor
                if (!ordersService.validateVendorAccess(userId, vendorId)) {
                    return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).build();
                }
            } catch (Exception e) {
                // Si no se puede validar, permitir acceso (para compatibilidad)
            }
        }
        return ResponseEntity.ok(ordersService.getOrdersByVendorId(vendorId));
    }

    @PostMapping("/{orderId}/ready")
    public ResponseEntity<OrderResponseDTO> markOrderAsReady(
            @PathVariable Long orderId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long vendorId = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                Long userId = ordersService.extractUserIdFromToken(token);
                vendorId = ordersService.getVendorIdByUserId(userId);
            } catch (Exception e) {
                // Si no se puede obtener, lanzará error en el servicio
            }
        }
        return ResponseEntity.ok(ordersService.markOrderAsReady(orderId, vendorId));
    }

    @PostMapping("/{orderId}/complete")
    public ResponseEntity<OrderResponseDTO> markOrderAsCompleted(
            @PathVariable Long orderId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long vendorId = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                Long userId = ordersService.extractUserIdFromToken(token);
                vendorId = ordersService.getVendorIdByUserId(userId);
            } catch (Exception e) {
                // Si no se puede obtener, lanzará error en el servicio
            }
        }
        return ResponseEntity.ok(ordersService.markOrderAsCompleted(orderId, vendorId));
    }

    /**
     * USUARIO: Cancela un pedido
     * Solo se puede cancelar si está en estado PENDIENTE_PAGO
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<OrderResponseDTO> cancelOrder(
            @PathVariable Long orderId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                userId = ordersService.extractUserIdFromToken(token);
            } catch (Exception e) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
            }
        } else {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(ordersService.cancelOrder(orderId, userId));
    }

}
