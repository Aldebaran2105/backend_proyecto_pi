package com.example.proyecto_pi3_backend.Orders.domain;

import com.example.proyecto_pi3_backend.Availability.domain.Availability;
import com.example.proyecto_pi3_backend.Availability.infrastructure.AvailabilityRepository;
import com.example.proyecto_pi3_backend.MenuItems.domain.MenuItems;
import com.example.proyecto_pi3_backend.MenuItems.infrastructure.MenuItemsRepository;
import com.example.proyecto_pi3_backend.OrderDetails.domain.OrderDetails;
import com.example.proyecto_pi3_backend.Orders.dto.OrderRequestDTO;
import com.example.proyecto_pi3_backend.Orders.dto.OrderResponseDTO;
import com.example.proyecto_pi3_backend.Orders.infrastructure.OrdersRepository;
import com.example.proyecto_pi3_backend.OrderDetails.infrastructure.OrderDetailsRepository;
import com.example.proyecto_pi3_backend.User.domain.Users;
import com.example.proyecto_pi3_backend.User.domain.Role;
import com.example.proyecto_pi3_backend.User.infrastructure.UserRepository;
import com.example.proyecto_pi3_backend.Vendors.domain.Vendors;
import com.example.proyecto_pi3_backend.Vendors.infrastructure.VendorsRepository;
import com.example.proyecto_pi3_backend.exception.ResourceNotFoundException;
import com.example.proyecto_pi3_backend.config.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de pedidos
 * 
 * Funcionalidades para USUARIO:
 * - Crear pedido (reservar)
 * - Pagar pedido con YAPE
 * - Ver historial de pedidos
 * 
 * Funcionalidades para VENDOR:
 * - Ver pedidos de su vendor
 * - Marcar pedido como listo para recoger
 * - Marcar pedido como completado
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrdersService {
    private final OrdersRepository ordersRepository;
    private final UserRepository userRepository;
    private final VendorsRepository vendorsRepository;
    private final MenuItemsRepository menuItemsRepository;
    private final OrderDetailsRepository orderDetailsRepository;
    private final AvailabilityRepository availabilityRepository;
    private final JwtService jwtService;
    
    /**
     * Extrae el userId del token JWT
     */
    public Long extractUserIdFromToken(String token) {
        return jwtService.extractUserId(token);
    }
    
    /**
     * Obtiene el vendorId de un usuario VENDOR
     */
    public Long getVendorIdByUserId(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));
        
        if (user.getRole() != Role.VENDOR || user.getVendor() == null) {
            throw new RuntimeException("El usuario no es un vendedor o no tiene un vendor asignado");
        }
        
        return user.getVendor().getId();
    }
    
    /**
     * Valida que un usuario tiene acceso a un vendor
     */
    public boolean validateVendorAccess(Long userId, Long vendorId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));
        
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        
        if (user.getRole() == Role.VENDOR) {
            return user.getVendor() != null && user.getVendor().getId().equals(vendorId);
        }
        
        return false;
    }

    /**
     * USUARIO: Crea un nuevo pedido (reserva)
     * Lógica: Crea pedido con estado PENDIENTE_PAGO, método de pago YAPE
     */
    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO orderRequestDTO) {
        validateOrderRequest(orderRequestDTO);
        
        Users user = userRepository.findById(orderRequestDTO.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + orderRequestDTO.getUserId()));
        
        Vendors vendor = vendorsRepository.findById(orderRequestDTO.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor no encontrado con ID: " + orderRequestDTO.getVendorId()));
        
        // Crear orden
        Orders order = new Orders();
        order.setUser(user);
        order.setVendor(vendor);
        order.setStatus("PENDIENTE_PAGO");
        
        // Establecer fecha de creación
        Timestamp now = new Timestamp(System.currentTimeMillis());
        order.setCreatedAt(now);
        
        // Calcular pickup_time usando el horario del vendor
        // Usar el horario de cierre del vendor como horario de recojo para el día del menú
        Timestamp pickupTime = calculatePickupTime(vendor, now);
        order.setPickup_time(pickupTime);
        
        order.setPickupCode(generatePickupCode());
        order.setPaymentMethod(orderRequestDTO.getPaymentMethod() != null ? orderRequestDTO.getPaymentMethod().toUpperCase() : "YAPE");
        
        order = ordersRepository.save(order);
        
        // Crear detalles de pedido
        List<OrderDetails> orderDetailsList = createOrderDetails(order, orderRequestDTO.getItems());
        
        return mapToDTO(order, orderDetailsList);
    }

    /**
     * USUARIO: Paga un pedido con YAPE
     * Lógica: Cambia estado de PENDIENTE_PAGO a PAGADO
     */
    @Transactional
    public OrderResponseDTO payOrder(Long orderId) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + orderId));
        
        if (!"PENDIENTE_PAGO".equals(order.getStatus())) {
            throw new RuntimeException("La orden no está pendiente de pago");
        }
        
        order.setStatus("PAGADO");
        order = ordersRepository.save(order);
        
        List<OrderDetails> orderDetails = getOrderDetails(orderId);
        return mapToDTO(order, orderDetails);
    }

    /**
     * USUARIO: Obtiene el historial de pedidos de un usuario
     */
    public List<OrderResponseDTO> getOrdersByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + userId);
        }
        
        List<Orders> orders = ordersRepository.findByUserId(userId);
        
        return orders.stream()
                .map(order -> {
                    List<OrderDetails> orderDetails = getOrderDetails(order.getId());
                    return mapToDTO(order, orderDetails);
                })
                .collect(Collectors.toList());
    }

    /**
     * VENDOR: Obtiene pedidos de su vendor
     */
    public List<OrderResponseDTO> getOrdersByVendorId(Long vendorId) {
        if (!vendorsRepository.existsById(vendorId)) {
            throw new ResourceNotFoundException("Vendor no encontrado con ID: " + vendorId);
        }
        
        List<Orders> orders = ordersRepository.findByVendorId(vendorId);
        
        return orders.stream()
                .map(order -> {
                    List<OrderDetails> orderDetails = getOrderDetails(order.getId());
                    return mapToDTO(order, orderDetails);
                })
                .collect(Collectors.toList());
    }

    /**
     * VENDOR: Marca un pedido como listo para recoger
     * Lógica: Solo puede marcarse si está PAGADO y pertenece al vendor del usuario
     */
    @Transactional
    public OrderResponseDTO markOrderAsReady(Long orderId, Long vendorId) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + orderId));
        
        // Validar acceso del vendor
        if (vendorId != null && !order.getVendor().getId().equals(vendorId)) {
            throw new RuntimeException("No tienes permiso para modificar este pedido");
        }
        
        if (!"PAGADO".equals(order.getStatus())) {
            throw new RuntimeException("La orden debe estar pagada para marcarla como lista");
        }
        
        order.setStatus("LISTO_PARA_RECOJO");
        order = ordersRepository.save(order);
        
        List<OrderDetails> orderDetails = getOrderDetails(orderId);
        return mapToDTO(order, orderDetails);
    }

    /**
     * VENDOR: Marca un pedido como completado
     * Lógica: Solo puede completarse si está LISTO_PARA_RECOJO
     */
    @Transactional
    public OrderResponseDTO markOrderAsCompleted(Long orderId, Long vendorId) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + orderId));
        
        // Validar acceso del vendor
        if (vendorId != null && !order.getVendor().getId().equals(vendorId)) {
            throw new RuntimeException("No tienes permiso para modificar este pedido");
        }
        
        if (!"LISTO_PARA_RECOJO".equals(order.getStatus())) {
            throw new RuntimeException("La orden debe estar lista para recoger");
        }
        
        order.setStatus("COMPLETADO");
        order = ordersRepository.save(order);
        
        List<OrderDetails> orderDetails = getOrderDetails(orderId);
        return mapToDTO(order, orderDetails);
    }

    /**
     * Obtiene un pedido por ID
     */
    public OrderResponseDTO getOrderById(Long orderId) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + orderId));
        
        List<OrderDetails> orderDetails = getOrderDetails(orderId);
        return mapToDTO(order, orderDetails);
    }

    /**
     * USUARIO: Cancela un pedido
     * Solo se puede cancelar si está en estado PENDIENTE_PAGO
     */
    @Transactional
    public OrderResponseDTO cancelOrder(Long orderId, Long userId) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + orderId));
        
        // Validar que el pedido pertenece al usuario
        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("No tienes permiso para cancelar este pedido");
        }
        
        return cancelOrderInternal(order);
    }
    
    /**
     * Cancela un pedido automáticamente (sin validar usuario)
     * Usado por el scheduler para cancelar pedidos expirados
     */
    @Transactional
    public void cancelOrderAutomatically(Long orderId) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + orderId));
        
        cancelOrderInternal(order);
    }
    
    /**
     * Lógica interna para cancelar un pedido y devolver stock
     */
    private OrderResponseDTO cancelOrderInternal(Orders order) {
        // Solo se puede cancelar si está pendiente de pago
        if (!"PENDIENTE_PAGO".equals(order.getStatus())) {
            throw new RuntimeException("Solo se pueden cancelar pedidos pendientes de pago. Estado actual: " + order.getStatus());
        }
        
        // Cambiar estado a CANCELADO
        order.setStatus("CANCELADO");
        order = ordersRepository.save(order);
        
        // Devolver stock si se había descontado (aunque en PENDIENTE_PAGO no debería haberse descontado)
        // Por seguridad, verificamos y devolvemos stock si es necesario
        Date orderDate = normalizeDate(new Date(order.getPickup_time().getTime()));
        List<OrderDetails> orderDetails = getOrderDetails(order.getId());
        
        for (OrderDetails detail : orderDetails) {
            try {
                returnStock(detail.getMenuItem(), orderDate, detail.getQuantity());
            } catch (Exception e) {
                log.warn("No se pudo devolver stock para el item {}: {}", detail.getMenuItem().getId(), e.getMessage());
            }
        }
        
        return mapToDTO(order, orderDetails);
    }

    // ========== MÉTODOS PRIVADOS DE APOYO ==========

    private void validateOrderRequest(OrderRequestDTO request) {
        if (request.getUserId() == null) {
            throw new RuntimeException("El usuario es requerido");
        }
        if (request.getVendorId() == null) {
            throw new RuntimeException("El vendor es requerido");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("El pedido debe tener al menos un item");
        }
        if (request.getPaymentMethod() == null || request.getPaymentMethod().trim().isEmpty()) {
            throw new RuntimeException("El método de pago es requerido");
        }
    }

    private List<OrderDetails> createOrderDetails(Orders order, List<OrderRequestDTO.OrderItemDTO> items) {
        List<OrderDetails> orderDetailsList = new ArrayList<>();
        
        // Obtener la fecha del pedido (normalizada a solo día, sin hora)
        Date orderDate = normalizeDate(new Date(order.getPickup_time().getTime()));
        
        for (OrderRequestDTO.OrderItemDTO itemDTO : items) {
            MenuItems menuItem = menuItemsRepository.findById(itemDTO.getMenuItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Menu item no encontrado con ID: " + itemDTO.getMenuItemId()));
            
            if (itemDTO.getQuantity() == null || itemDTO.getQuantity() <= 0) {
                throw new RuntimeException("La cantidad debe ser mayor a 0");
            }
            
            // Verificar y descontar stock
            deductStock(menuItem, orderDate, itemDTO.getQuantity());
            
            OrderDetails orderDetail = new OrderDetails();
            orderDetail.setOrder(order);
            orderDetail.setMenuItem(menuItem);
            orderDetail.setQuantity(itemDTO.getQuantity());
            
            orderDetailsList.add(orderDetailsRepository.save(orderDetail));
        }
        
        return orderDetailsList;
    }
    
    /**
     * Devuelve stock de un menú para una fecha específica (usado al cancelar pedidos)
     */
    private void returnStock(MenuItems menuItem, Date date, Integer quantity) {
        Date normalizedDate = normalizeDate(date);
        
        List<Availability> allAvailabilities = availabilityRepository.findAll();
        Optional<Availability> existingAvailability = allAvailabilities.stream()
                .filter(avail -> avail.getMenuItem().getId().equals(menuItem.getId()) && 
                               isSameDay(avail.getDate(), normalizedDate))
                .findFirst();
        
        if (existingAvailability.isPresent()) {
            Availability availability = existingAvailability.get();
            Integer currentStock = availability.getStock() != null ? availability.getStock() : 0;
            int newStock = currentStock + quantity;
            availability.setStock(newStock);
            
            // Si el stock vuelve a ser mayor a 0, marcar como disponible
            if (newStock > 0 && !Boolean.TRUE.equals(availability.getIsAvailable())) {
                availability.setIsAvailable(true);
            }
            
            availabilityRepository.save(availability);
        }
    }

    /**
     * Descuenta el stock de un menú para una fecha específica
     */
    private void deductStock(MenuItems menuItem, Date date, Integer quantity) {
        // Normalizar la fecha (solo día, sin hora)
        Date normalizedDate = normalizeDate(date);
        
        // Buscar disponibilidad existente para esta fecha
        List<Availability> allAvailabilities = availabilityRepository.findAll();
        Optional<Availability> existingAvailability = allAvailabilities.stream()
                .filter(avail -> avail.getMenuItem().getId().equals(menuItem.getId()) && 
                               isSameDay(avail.getDate(), normalizedDate))
                .findFirst();
        
        // Si no existe disponibilidad para esta fecha, el item no está disponible
        if (!existingAvailability.isPresent()) {
            throw new RuntimeException(
                String.format("El item '%s' no está disponible para esta fecha", menuItem.getItemName())
            );
        }
        
        Availability availability = existingAvailability.get();
        
        // Verificar que el item está disponible
        if (!Boolean.TRUE.equals(availability.getIsAvailable())) {
            throw new RuntimeException(
                String.format("El item '%s' no está disponible para esta fecha", menuItem.getItemName())
            );
        }
        
        // Verificar que hay suficiente stock
        Integer currentStock = availability.getStock() != null ? availability.getStock() : 0;
        if (currentStock < quantity) {
            throw new RuntimeException(
                String.format("Stock insuficiente para '%s'. Stock disponible: %d, solicitado: %d", 
                    menuItem.getItemName(), currentStock, quantity)
            );
        }
        
        // Descontar stock
        int newStock = currentStock - quantity;
        availability.setStock(newStock);
        
        // Si el stock llega a 0, marcar como no disponible
        if (newStock <= 0) {
            availability.setIsAvailable(false);
        }
        
        availabilityRepository.save(availability);
    }
    
    /**
     * Normaliza una fecha eliminando la hora (solo día)
     */
    private Date normalizeDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
    
    /**
     * Verifica si dos fechas son del mismo día
     */
    private boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private List<OrderDetails> getOrderDetails(Long orderId) {
        return orderDetailsRepository.findByOrderId(orderId);
    }

    /**
     * Calcula el horario de recojo basado en el horario del vendor
     * Usa el horario de cierre del vendor para el día del menú
     */
    private Timestamp calculatePickupTime(Vendors vendor, Timestamp orderDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(orderDate.getTime());
        
        // Obtener el horario de cierre del vendor (horario de recojo)
        // Si no tiene horario configurado, usar 18:00 por defecto
        int hour = 18;
        int minute = 0;
        
        if (vendor.getClosingTime() != null) {
            hour = vendor.getClosingTime().getHour();
            minute = vendor.getClosingTime().getMinute();
        }
        
        // Establecer la hora de recojo para el día del menú
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        
        return new Timestamp(cal.getTimeInMillis());
    }

    private String generatePickupCode() {
        return "ORD-" + String.format("%05d", System.currentTimeMillis() % 100000);
    }

    private OrderResponseDTO mapToDTO(Orders order, List<OrderDetails> orderDetails) {
        List<OrderResponseDTO.OrderDetailResponseDTO> items = orderDetails.stream()
                .map(od -> new OrderResponseDTO.OrderDetailResponseDTO(
                        od.getId(),
                        od.getMenuItem().getItemName(),
                        od.getQuantity(),
                        od.getMenuItem().getPrice(),
                        od.getMenuItem().getId()
                ))
                .collect(Collectors.toList());
        
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        dto.setStatus(order.getStatus());
        dto.setPickup_time(order.getPickup_time());
        dto.setUserId(order.getUser().getId());
        dto.setUserName(order.getUser().getFirstName() + " " + order.getUser().getLastName());
        dto.setVendorId(order.getVendor().getId());
        dto.setVendorName(order.getVendor().getName());
        dto.setPickupCode(order.getPickupCode());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setMercadoPagoPaymentId(order.getMercadoPagoPaymentId());
        dto.setMercadoPagoPreferenceId(order.getMercadoPagoPreferenceId());
        dto.setItems(items);
        return dto;
    }
}
