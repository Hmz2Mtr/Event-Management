//package com.example.Event_Management.Controllers;
//
//
//import com.example.Event_Management.entities.Event;
//import com.example.Event_Management.entities.Session;
//import com.example.Event_Management.repository.EventRepository;
//import com.example.Event_Management.repository.SessionRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.time.LocalDate;
//import java.time.LocalTime;
//import java.util.ArrayList;
//import java.util.List;
//
//@Controller
////@RequestMapping("/api/events")
//public class EventController {
//
//    @Autowired
//    private EventRepository eventRepository;
//    @Autowired
//    private SessionRepository sessionRepository;
//
//    @GetMapping("/createEvent")
//    public String createEventPage() {
//        return "events/createEvent"; // Returns the createEvent.html page
//    }
//    @PostMapping("/createEvent")
//    public ResponseEntity<String> createEvent(
//            @RequestParam("name") String name,
//            @RequestParam("description") String description,
//            @RequestParam("image") MultipartFile image,
//            @RequestParam(value = "sessions[0].date", required = false) String[] sessionDates,
//            @RequestParam(value = "sessions[0].startTime", required = false) String[] startTimes,
//            @RequestParam(value = "sessions[0].endTime", required = false) String[] endTimes
//    ) {
//        try {
//            // Create and save the event
//            Event event = new Event();
//            event.setName(name);
//            event.setDescription(description);
//            event.setImage(image.getBytes());
//            event = eventRepository.save(event);
//
//            // Create and save sessions
//            List<Session> sessions = new ArrayList<>();
//            if (sessionDates != null) {
//                for (int i = 0; i < sessionDates.length; i++) {
//                    Session session = new Session();
//                    session.setDate(LocalDate.parse(sessionDates[i]));
//                    session.setStartTime(LocalTime.parse(startTimes[i]));
//                    session.setEndTime(LocalTime.parse(endTimes[i]));
//                    session.setEvent(event);
//                    sessions.add(session);
//                }
//                sessionRepository.saveAll(sessions);
//            }
//
//            return ResponseEntity.ok("Event created successfully");
//        } catch (IOException e) {
//            return ResponseEntity.badRequest().body("Error processing image: " + e.getMessage());
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body("Error creating event: " + e.getMessage());
//        }
//    }
//
//
//    @GetMapping("/eventss")
//    public String getAllEvents(Model model) {
//        List<Event> events = eventRepository.findAll();
//        model.addAttribute("events", events);
//        return "events/events"; // Redirects to events.html
//    }
//
//
//
//
//    @GetMapping("/events/{id}")
//    public ResponseEntity<Event> getEventById(@PathVariable Long id) {
//        Event event = eventRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Event not found with ID: " + id));
//        return ResponseEntity.ok(event);
//    }
//
//
//
//}