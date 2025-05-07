package com.example.Event_Management.Controllers;


import com.example.Event_Management.entities.Event;
import com.example.Event_Management.entities.Session;
import com.example.Event_Management.repository.EventRepository;
import com.example.Event_Management.repository.SessionRepository;
import com.example.Event_Management.security.TokenDecoder.JwtTokenDecoder;
import com.example.Event_Management.security.entities.AppUser;
import com.example.Event_Management.security.services.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.example.Event_Management.entities.EventType;

import static com.example.Event_Management.security.Controller.AccountRestController.addAuthButtonAttributes;

import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;

@Controller
//@RequestMapping("/api/events")
public class EventController {

    private EventRepository eventRepository;
    private SessionRepository sessionRepository;
    private JwtTokenDecoder jwtTokenDecoder;
    private AccountService accountService;


    public EventController(EventRepository eventRepository, SessionRepository sessionRepository, JwtTokenDecoder jwtTokenDecoder, AccountService accountService) {
        this.eventRepository = eventRepository;
        this.sessionRepository = sessionRepository;
        this.jwtTokenDecoder = jwtTokenDecoder;
        this.accountService = accountService;
    }

    @GetMapping("/createEvent")
    public String createEventPage(Model model, HttpServletRequest request) {
        // Decode JWT using JwtTokenDecoder
        Map<String, Object> userInfo = jwtTokenDecoder.decodeToken(request);
        model.addAttribute("username", userInfo.get("username"));
        model.addAttribute("roles", userInfo.get("roles"));
        addAuthButtonAttributes(model, userInfo);

        return "Events/createEvent"; // Returns the createEvent.html page
    }


    @PostMapping("/createEvent")
//    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<String> createEvent(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("eventType") String eventType,
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "sessions[0].date", required = false) String[] sessionDates,
            @RequestParam(value = "sessions[0].startTime", required = false) String[] startTimes,
            @RequestParam(value = "sessions[0].endTime", required = false) String[] endTimes,
            Model model, HttpServletRequest request, Principal principal

            ) {
        // Decode JWT using JwtTokenDecoder
        Map<String, Object> userInfo = jwtTokenDecoder.decodeToken(request);
        model.addAttribute("username", userInfo.get("username"));
        model.addAttribute("roles", userInfo.get("roles"));
        addAuthButtonAttributes(model, userInfo);

        try {
            // Get authenticated user
            AppUser Creator = accountService.loadUserByUsername((String) userInfo.get("username"));
            if (Creator == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            // Create and save the event
            Event event = new Event();
            event.setName(name);
            event.setDescription(description);
            event.setEventType(EventType.valueOf(eventType));
            event.setCreatedBy(Creator);
            // Process file upload
            if (image != null && !image.isEmpty()) {
                event.setImage(image.getBytes());
            }
            event.setCreatorName(Creator.getUsername());
            eventRepository.save(event);


            // Create and save sessions
            List<Session> sessions = new ArrayList<>();
            if (sessionDates != null) {
                for (int i = 0; i < sessionDates.length; i++) {
                    Session session = new Session();
                    session.setDate(LocalDate.parse(sessionDates[i]));
                    session.setStartTime(LocalTime.parse(startTimes[i]));
                    session.setEndTime(LocalTime.parse(endTimes[i]));
                    session.setEvent(event);
                    sessions.add(session);
                }
                sessionRepository.saveAll(sessions);
            }

//            event.setSessions(sessions);
//            eventRepository.save(event);

            return ResponseEntity.ok("Event created successfully");
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error processing image: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating event: " + e.getMessage());
        }
    }


    @GetMapping("/events")
    public String getAllEvents(Model model, HttpServletRequest request) {
        // Decode JWT using JwtTokenDecoder
        Map<String, Object> userInfo = jwtTokenDecoder.decodeToken(request);
        model.addAttribute("username", userInfo.get("username"));
        model.addAttribute("roles", userInfo.get("roles"));
        addAuthButtonAttributes(model, userInfo);


        List<Event> events = eventRepository.findAll();
        model.addAttribute("events", events);
        return "Events/events"; // Redirects to events.html
    }



    @GetMapping("/events/{id}")
    public ResponseEntity<Event> getEventById(@PathVariable Long id, Model model, HttpServletRequest request) {
        // Decode JWT using JwtTokenDecoder
        Map<String, Object> userInfo = jwtTokenDecoder.decodeToken(request);
        model.addAttribute("username", userInfo.get("username"));
        model.addAttribute("roles", userInfo.get("roles"));
        addAuthButtonAttributes(model, userInfo);

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with ID: " + id));
        return ResponseEntity.ok(event);
    }



}