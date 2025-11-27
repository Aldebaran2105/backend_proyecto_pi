package com.example.proyecto_pi3_backend.Feedback.domain;

import com.example.proyecto_pi3_backend.Feedback.dto.FeedbackRequestDTO;
import com.example.proyecto_pi3_backend.Feedback.dto.FeedbackResponseDTO;
import com.example.proyecto_pi3_backend.Feedback.infrastructure.FeedbackRepository;
import com.example.proyecto_pi3_backend.MenuItems.domain.MenuItems;
import com.example.proyecto_pi3_backend.MenuItems.infrastructure.MenuItemsRepository;
import com.example.proyecto_pi3_backend.Orders.domain.Orders;
import com.example.proyecto_pi3_backend.Orders.infrastructure.OrdersRepository;
import com.example.proyecto_pi3_backend.User.domain.Users;
import com.example.proyecto_pi3_backend.User.infrastructure.UserRepository;
import com.example.proyecto_pi3_backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de feedback (comentarios)
 * 
 * Funcionalidades para USUARIO:
 * - Dejar comentarios en menús
 * 
 * Funcionalidades para ADMIN:
 * - Ver todos los comentarios de los menús
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final OrdersRepository ordersRepository;
    private final MenuItemsRepository menuItemsRepository;

    /**
     * USUARIO: Crea un comentario sobre un menú
     * Lógica: El usuario puede comentar sobre un menú que haya pedido, pero solo un comentario por pedido
     */
    @Transactional
    public FeedbackResponseDTO createFeedback(FeedbackRequestDTO requestDTO) {
        if (requestDTO.getRating() == null || requestDTO.getRating() < 1 || requestDTO.getRating() > 5) {
            throw new RuntimeException("La calificación debe estar entre 1 y 5");
        }
        
        if (requestDTO.getUserId() == null) {
            throw new RuntimeException("El usuario es requerido");
        }
        
        if (requestDTO.getOrderId() == null) {
            throw new RuntimeException("El pedido es requerido para dejar un comentario");
        }
        
        Users user = userRepository.findById(requestDTO.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + requestDTO.getUserId()));

        Orders order = ordersRepository.findById(requestDTO.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con ID: " + requestDTO.getOrderId()));
        
        if (!order.getUser().getId().equals(requestDTO.getUserId())) {
            throw new RuntimeException("No puedes comentar sobre un pedido que no es tuyo");
        }
        
        List<Feedback> existingFeedbacks = feedbackRepository.findAll().stream()
                .filter(fb -> fb.getOrder() != null && 
                             fb.getOrder().getId().equals(requestDTO.getOrderId()) &&
                             fb.getUser().getId().equals(requestDTO.getUserId()))
                .collect(Collectors.toList());
        
        if (!existingFeedbacks.isEmpty()) {
            throw new RuntimeException("Ya has dejado un comentario para este pedido. Solo puedes dejar un comentario por pedido.");
        }

        if (requestDTO.getMenuItemId() == null) {
            throw new RuntimeException("Debes especificar un menú para comentar");
        }
        
        MenuItems menuItem = menuItemsRepository.findById(requestDTO.getMenuItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu item no encontrado con ID: " + requestDTO.getMenuItemId()));

        Feedback feedback = new Feedback();
        feedback.setRating(requestDTO.getRating());
        feedback.setComment(requestDTO.getComment() != null ? requestDTO.getComment().trim() : null);
        feedback.setUser(user);
        feedback.setOrder(order);
        feedback.setMenuItem(menuItem);
        feedback.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        
        feedback = feedbackRepository.save(feedback);
        
        return mapToDTO(feedback);
    }

    /**
     * ADMIN: Obtiene todos los comentarios de todos los menús
     */
    public List<FeedbackResponseDTO> getAllFeedbacks() {
        try {
            List<Feedback> allFeedbacks = feedbackRepository.findAll();
            log.info("Encontrados {} comentarios", allFeedbacks.size());
            return allFeedbacks.stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error al obtener todos los comentarios", e);
            throw e;
        }
    }

    /**
     * VENDOR: Obtiene comentarios solo de menús que pertenecen a su vendor
     */
    public List<FeedbackResponseDTO> getFeedbacksByVendor(Long vendorId) {
        try {
            List<Feedback> allFeedbacks = feedbackRepository.findAll();
            log.info("Buscando comentarios para vendorId: {}, total comentarios: {}", vendorId, allFeedbacks.size());
            List<FeedbackResponseDTO> result = allFeedbacks.stream()
                    .filter(fb -> fb.getMenuItem() != null && 
                                 fb.getMenuItem().getVendor() != null &&
                                 fb.getMenuItem().getVendor().getId().equals(vendorId))
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());
            log.info("Encontrados {} comentarios para vendorId: {}", result.size(), vendorId);
            return result;
        } catch (Exception e) {
            log.error("Error al obtener comentarios por vendor", e);
            throw e;
        }
    }

    /**
     * Obtiene comentarios por item del menú
     */
    public List<FeedbackResponseDTO> getFeedbackByMenuItem(Long menuItemId) {
        if (!menuItemsRepository.existsById(menuItemId)) {
            throw new ResourceNotFoundException("Menu item no encontrado con ID: " + menuItemId);
        }
        
        return feedbackRepository.findAll().stream()
                .filter(fb -> fb.getMenuItem() != null && fb.getMenuItem().getId().equals(menuItemId))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene comentarios por usuario
     */
    public List<FeedbackResponseDTO> getFeedbackByUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Usuario no encontrado con ID: " + userId);
        }
        
        return feedbackRepository.findAll().stream()
                .filter(fb -> fb.getUser().getId().equals(userId))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Mapea Feedback a FeedbackResponseDTO
     * Los comentarios son anónimos, no se muestra el nombre del usuario
     */
    private FeedbackResponseDTO mapToDTO(Feedback feedback) {
        String itemName = feedback.getMenuItem() != null ? feedback.getMenuItem().getItemName() : "N/A";
        String vendorName = feedback.getMenuItem() != null && 
                           feedback.getMenuItem().getVendor() != null ? 
                           feedback.getMenuItem().getVendor().getName() : "N/A";
        
        return new FeedbackResponseDTO(
                feedback.getId(),
                feedback.getRating(),
                feedback.getComment(),
                itemName,
                vendorName,
                feedback.getCreatedAt()
        );
    }

    /**
     * ADMIN: Elimina un comentario
     */
    @Transactional
    public void deleteFeedback(Long id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comentario no encontrado con ID: " + id));
        feedbackRepository.delete(feedback);
    }
}
