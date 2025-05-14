package com.example.Event_Management.Controllers;

import com.example.Event_Management.entities.event.Event;
import com.example.Event_Management.entities.invitation.InvitationSession;
import com.example.Event_Management.repository.event.EventRepository;
import com.example.Event_Management.security.entities.AppUser;
import com.example.Event_Management.security.repository.AppUserRepository;
import com.example.Event_Management.services.FaceRecognitionService;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@CrossOrigin("*")
public class FaceController {
    private static final Logger logger = LoggerFactory.getLogger(FaceController.class);
    private final EventRepository eventRepository;
    private final AppUserRepository appUserRepository;
    private final FaceRecognitionService faceRecognitionService;

    public FaceController(EventRepository eventRepository,
                         AppUserRepository appUserRepository,
                         FaceRecognitionService faceRecognitionService) {
        this.eventRepository = eventRepository;
        this.appUserRepository = appUserRepository;
        this.faceRecognitionService = faceRecognitionService;
    }

    @GetMapping("/scan/{eventId}/{sessionId}")
    public String showScanPage(@PathVariable Long eventId, @PathVariable Long sessionId, Model model) {
        try {
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new RuntimeException("Event not found"));
            model.addAttribute("event", event);
            model.addAttribute("sessionId", sessionId);
            return "admin/face-scan";
        } catch (Exception e) {
            logger.error("Error showing scan page: ", e);
            model.addAttribute("error", "Error loading scan page: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/api/scan/{eventId}/{sessionId}")
    @ResponseBody
    @CrossOrigin("*")
    public ResponseEntity<?> verifyFaceForSession(@PathVariable Long eventId,
                                                 @PathVariable Long sessionId,
                                                 @RequestBody Map<String, String> requestBody) {
        try {
            String imageBase64 = requestBody.get("image");
            if (imageBase64 == null || imageBase64.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "No image data provided"
                ));
            }

            // Clean up the base64 string by removing the data URL prefix if present
            if (imageBase64.contains(",")) {
                imageBase64 = imageBase64.split(",")[1];
            }
            
            // Remove any whitespace or newlines that might have been added
            imageBase64 = imageBase64.trim();

            // Get event and session invitees
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new RuntimeException("Event not found"));
            
            // Get invitees for the specific session
            List<String> inviteesUsername = event.getSessions().stream()
                    .filter(session -> session.getId().equals(sessionId))
                    .flatMap(session -> session.getInvitationSessions().stream())
                    .map(InvitationSession::getUsername)
                    .distinct()
                    .collect(Collectors.toList());

            if (inviteesUsername.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "No invitees found for this session"
                ));
            }
            
            List<AppUser> invitees = appUserRepository.findByUsernameIn(inviteesUsername);
            
            if (invitees.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "No registered users found among session invitees"
                ));
            }

            // Process the captured image
            byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
            Mat capturedImage = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_COLOR);
            
            if (capturedImage.empty()) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Failed to process captured image"
                ));
            }

            // Detect and align face in the captured image
            Mat alignedFace = faceRecognitionService.detectAndAlignFace(capturedImage);
            if (alignedFace == null) {
                capturedImage.release();
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "No face detected. Please position your face in front of the camera.",
                    "shouldContinue", true
                ));
            }

            // Compare with each invitee's face
            AppUser matchedUser = null;
            double bestMatchScore = 0.0;
            
            for (AppUser invitee : invitees) {
                if (invitee.getProfilePicture() == null) continue;

                // Clean up the profile picture base64 string
                String profileBase64 = invitee.getProfilePicture();
                if (profileBase64.contains(",")) {
                    profileBase64 = profileBase64.split(",")[1];
                }
                profileBase64 = profileBase64.trim();

                byte[] profileBytes = Base64.getDecoder().decode(profileBase64);
                Mat profileImage = Imgcodecs.imdecode(new MatOfByte(profileBytes), Imgcodecs.IMREAD_COLOR);
                
                if (profileImage.empty()) {
                    logger.warn("Failed to decode profile picture for user: {}", invitee.getUsername());
                    continue;
                }

                Mat alignedProfile = faceRecognitionService.detectAndAlignFace(profileImage);
                if (alignedProfile != null) {
                    double similarity = faceRecognitionService.compareFacesWithScore(alignedFace, alignedProfile);
                    logger.debug("Similarity score for user {}: {}", invitee.getUsername(), similarity);
                    
                    if (similarity > bestMatchScore) {
                        bestMatchScore = similarity;
                        matchedUser = invitee;
                    }
                    alignedProfile.release();
                }
                profileImage.release();
            }

            alignedFace.release();
            capturedImage.release();

            if (matchedUser != null && bestMatchScore >= FaceRecognitionService.HIGH_CONFIDENCE_THRESHOLD) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Welcome " + matchedUser.getUsername() + "!",
                    "user", matchedUser.getUsername(),
                    "confidence", bestMatchScore,
                    "shouldContinue", true
                ));
            }

            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Person not recognized or not invited to this session",
                "shouldContinue", true
            ));

        } catch (Exception e) {
            logger.error("Error during face verification: ", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Error processing face recognition: " + e.getMessage(),
                "shouldContinue", true
            ));
        }
    }
}