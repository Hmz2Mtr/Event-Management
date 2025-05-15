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
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Controller
@CrossOrigin("*")
public class FaceController {
    private static final Logger logger = LoggerFactory.getLogger(FaceController.class);
    private final EventRepository eventRepository;
    private final AppUserRepository appUserRepository;
    private final FaceRecognitionService faceRecognitionService;
    private final Map<String, Long> lastRecognitionTimes = new ConcurrentHashMap<>();
    private final Map<String, String> lastRecognizedUsers = new ConcurrentHashMap<>();
    private static final long RECOGNITION_COOLDOWN = 2000; // 2 seconds cooldown

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
    public ResponseEntity<?> scanFace(@PathVariable Long eventId,
                                    @PathVariable Long sessionId,
                                    @RequestBody Map<String, String> request) {
        String sessionKey = eventId + "-" + sessionId;
        long currentTime = System.currentTimeMillis();
        
        // Clear previous recognition if it's been more than the cooldown period
        Long lastRecognitionTime = lastRecognitionTimes.get(sessionKey);
        if (lastRecognitionTime != null && currentTime - lastRecognitionTime > RECOGNITION_COOLDOWN) {
            lastRecognizedUsers.remove(sessionKey);
        }

        try {
            String imageBase64 = request.get("image").split(",")[1];
            
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
                return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of(
                        "success", false,
                        "message", "No invitees found for this session",
                        "shouldContinue", false
                    ));
            }
            
            List<AppUser> invitees = appUserRepository.findByUsernameIn(inviteesUsername);
            
            if (invitees.isEmpty()) {
                return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of(
                        "success", false,
                        "message", "No registered users found among session invitees",
                        "shouldContinue", false
                    ));
            }

            // Process the captured image
            byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
            Mat capturedImage = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_COLOR);
            
            if (capturedImage.empty()) {
                return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of(
                        "success", false,
                        "message", "Failed to process captured image",
                        "shouldContinue", true
                    ));
            }

            // Detect and align face in the captured image
            Mat alignedFace = faceRecognitionService.detectAndAlignFace(capturedImage);
            if (alignedFace == null) {
                capturedImage.release();
                return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of(
                        "success", false,
                        "message", "No face detected. Please position your face in front of the camera.",
                        "shouldContinue", true
                    ));
            }

            // Compare with each invitee's face
            AppUser matchedUser = null;
            double bestMatchScore = 0.0;

            // Check if we already recognized someone recently
            String lastRecognizedUser = lastRecognizedUsers.get(sessionKey);
            if (lastRecognizedUser != null) {
                capturedImage.release();
                alignedFace.release();
                return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of(
                        "success", true,
                        "message", "Welcome " + lastRecognizedUser + "!",
                        "user", lastRecognizedUser,
                        "shouldContinue", false
                    ));
            }

            for (AppUser invitee : invitees) {
                if (invitee.getProfilePicture() == null) continue;

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
                // Store the recognition
                lastRecognitionTimes.put(sessionKey, currentTime);
                lastRecognizedUsers.put(sessionKey, matchedUser.getUsername());
                
                return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of(
                        "success", true,
                        "message", "Welcome " + matchedUser.getUsername() + "!",
                        "user", matchedUser.getUsername(),
                        "confidence", bestMatchScore,
                        "shouldContinue", false
                    ));
            }

            return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of(
                    "success", false,
                    "message", "Face not recognized or not invited to this session",
                    "shouldContinue", true
                ));

        } catch (Exception e) {
            logger.error("Error during face scanning: ", e);
            return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of(
                    "success", false,
                    "message", "Error during face scanning: " + e.getMessage(),
                    "shouldContinue", true
                ));
        }
    }
}