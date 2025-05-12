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
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class FaceController {
    private EventRepository eventRepository;
    private AppUserRepository appUserRepository;
    private FaceRecognitionService faceRecognitionService;

    public FaceController(EventRepository eventRepository, AppUserRepository appUserRepository, FaceRecognitionService faceRecognitionService) {
        this.eventRepository = eventRepository;
        this.appUserRepository = appUserRepository;
        this.faceRecognitionService = faceRecognitionService;
    }


//    @PostMapping("/scan")
//    public ResponseEntity<?> verifyFaceForEvent(@RequestBody Map<String, String> request) {
//        String imageBase64 = request.get("image");
//        String eventIdStr = request.get("eventId");
//
//        if (imageBase64 == null || eventIdStr == null) {
//            return ResponseEntity.badRequest().body("Missing image or eventId");
//        }
//
//        try {
//            Long eventId = Long.parseLong(eventIdStr);
//            Event event = eventRepository.findById(eventId)
//                    .orElseThrow(() -> new RuntimeException("Event not found"));
//
//            // Get invitees for the event
//            List<String> inviteeUsernames = event.getSessions().stream()
//                    .flatMap(session -> session.getInvitationSessions().stream())
//                    .map(InvitationSession::getInvitee)
//                    .distinct()
//                    .collect(Collectors.toList());
//
//            List<AppUser> invitees = appUserRepository.findByUsernameIn(inviteeUsernames);
//
//            if (invitees.isEmpty()) {
//                return ResponseEntity.ok(Map.of(
//                        "verified", false,
//                        "message", "No invitees found for this event"
//                ));
//            }
//
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



//    @GetMapping("/index2")
//    public String index() {
//        return "index2";
//    }
//
//    @PostMapping("/store")
//    public String storeFace(@RequestBody Map<String, String> data) {
//        String base64 = data.get("image");
//        String name = data.get("name");
//        faceService.storeFace(base64, name);
//        return "Stored";
//    }
//
//    @PostMapping("/recognize")
//    public String recognizeFace(@RequestBody Map<String, String> data) {
//        String base64 = data.get("image");
//        return faceService.recognizeFace(base64);
//    }
}