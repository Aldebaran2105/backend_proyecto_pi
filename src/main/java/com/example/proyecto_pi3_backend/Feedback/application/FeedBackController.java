package com.example.proyecto_pi3_backend.Feedback.application;

import com.example.proyecto_pi3_backend.Feedback.domain.FeedbackService;
import com.example.proyecto_pi3_backend.Feedback.dto.FeedbackRequestDTO;
import com.example.proyecto_pi3_backend.Feedback.dto.FeedbackResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/feedback")
@RequiredArgsConstructor
public class FeedBackController {
    private final FeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<FeedbackResponseDTO> createFeedback(@RequestBody FeedbackRequestDTO feedbackRequestDTO) {
        return ResponseEntity.ok(feedbackService.createFeedback(feedbackRequestDTO));
    }

    @GetMapping
    public ResponseEntity<List<FeedbackResponseDTO>> getAllFeedbacks() {
        try {
            return ResponseEntity.ok(feedbackService.getAllFeedbacks());
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/item/{menuItemId}")
    public ResponseEntity<List<FeedbackResponseDTO>> getFeedbackByMenuItem(@PathVariable Long menuItemId) {
        return ResponseEntity.ok(feedbackService.getFeedbackByMenuItem(menuItemId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<FeedbackResponseDTO>> getFeedbackByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(feedbackService.getFeedbackByUser(userId));
    }

    @GetMapping("/vendor/{vendorId}")
    public ResponseEntity<List<FeedbackResponseDTO>> getFeedbacksByVendor(@PathVariable Long vendorId) {
        try {
            return ResponseEntity.ok(feedbackService.getFeedbacksByVendor(vendorId));
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeedback(@PathVariable Long id) {
        feedbackService.deleteFeedback(id);
        return ResponseEntity.noContent().build();
    }
}
