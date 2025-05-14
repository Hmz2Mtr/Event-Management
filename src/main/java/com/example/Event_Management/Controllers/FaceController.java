package com.example.Event_Management.Controllers;

import com.example.Event_Management.entities.event.Event;
import com.example.Event_Management.entities.invitation.InvitationSession;
import com.example.Event_Management.repository.event.EventRepository;
import com.example.Event_Management.security.TokenDecoder.JwtTokenDecoder;
import com.example.Event_Management.security.entities.AppUser;
import com.example.Event_Management.security.repository.AppUserRepository;
import com.example.Event_Management.security.services.AccountService;
import com.example.Event_Management.services.FaceRecognitionService;
import jakarta.servlet.http.HttpServletRequest;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class FaceController {
    private EventRepository eventRepository;
    private AppUserRepository appUserRepository;
    private FaceRecognitionService faceRecognitionService;
    private JwtTokenDecoder jwtTokenDecoder;
    private AccountService accountService;

    public FaceController(EventRepository eventRepository, AppUserRepository appUserRepository, FaceRecognitionService faceRecognitionService, JwtTokenDecoder jwtTokenDecoder, AccountService accountService) {
        this.eventRepository = eventRepository;
        this.appUserRepository = appUserRepository;
        this.faceRecognitionService = faceRecognitionService;
        this.jwtTokenDecoder = jwtTokenDecoder;
        this.accountService = accountService;
    }

//    @PostMapping("/scan/{eventId}")
//    public ResponseEntity<?> verifyFaceForEvent(@PathVariable Long eventId,
//                                                HttpServletRequest request,
//                                                Model model,
//                                                Map<String, Object> requestBody) {
//        Map<String, Object> userInfo = jwtTokenDecoder.decodeToken(request);
//        String username = (String) userInfo.get("username");
//        AppUser appUser = accountService.loadUserByUsername(username);
//
//        String imageBase64 = (String) requestBody.get("image");
//
//        if (imageBase64 == null) {
//            return ResponseEntity.badRequest().body("Missing image.");
//        }
//
//        try {
//            // Fetch the event and its invitees
//            Event event = eventRepository.findById(eventId)
//                    .orElseThrow(() -> new RuntimeException("Event not found"));
//            List<String> inveteesUsername = event.getSessions().stream()
//                    .flatMap(session -> session.getInvitationSessions().stream())
//                    .map(InvitationSession::getInvitee)
//                    .distinct()
//                    .collect(Collectors.toList());
//            List<AppUser> invitees = appUserRepository.findByUsernameIn(inveteesUsername);
//
//            if (invitees.isEmpty()) {
//                return ResponseEntity.ok(Map.of(
//                        "verified", false,
//                        "message", "No invitees found for this event"
//                ));
//            }

//            // Decode the captured image
//            byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
//            Mat capturedMat = Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_COLOR);
//
//            // Generate embedding for the captured image
//            double[] capturedEmbedding = faceRecognitionService.generateEmbedding(capturedMat);
//
//            // Compare with invitees' profile pictures
//            for (AppUser invitee : invitees) {
//                if (invitee.getProfilePicture() == null) continue;
//
//                byte[] profilePictureBytes = Base64.getDecoder().decode(invitee.getProfilePicture());
//                Mat profileMat = Imgcodecs.imdecode(new MatOfByte(profilePictureBytes), Imgcodecs.IMREAD_COLOR);
//                double[] profileEmbedding = faceRecognitionService.generateEmbedding(profileMat);
//
//                double distance = faceRecognitionService.computeDistance(capturedEmbedding, profileEmbedding);
//                if (distance < 0.6) { // Threshold for FaceNet
//                    return ResponseEntity.ok(Map.of(
//                            "verified", true,
//                            "message", "Invitee verified: " + invitee.getUsername()
//                    ));
//                }
//            }
//
//            return ResponseEntity.ok(Map.of(
//                    "verified", false,
//                    "message", "No matching invitee found"
//            ));
//
//        } catch (NumberFormatException e) {
//            return ResponseEntity.badRequest().body("Invalid eventId format");
//        } catch (Exception e) {
//            return ResponseEntity.status(500).body("Error during verification: " + e.getMessage());
//        }
//    }
}