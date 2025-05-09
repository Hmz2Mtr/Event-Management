package com.example.Event_Management.Controllers;


import com.example.Event_Management.DTO.event.EventDTO;
import com.example.Event_Management.DTO.event.PlaceDTO;
import com.example.Event_Management.DTO.event.SessionDTO;
import com.example.Event_Management.DTO.event.VilleDTO;
import com.example.Event_Management.DTO.invitation.InvitationFormDTO;
import com.example.Event_Management.entities.event.*;
import com.example.Event_Management.entities.invitation.InvitationForm;
import com.example.Event_Management.entities.invitation.InvitationSession;
import com.example.Event_Management.repository.event.EventRepository;
import com.example.Event_Management.repository.event.PlaceRepository;
import com.example.Event_Management.repository.event.SessionRepository;
import com.example.Event_Management.repository.event.VilleRepository;
import com.example.Event_Management.repository.invitation.InvitationFormRepository;
import com.example.Event_Management.repository.invitation.InvitationSessionRepository;
import com.example.Event_Management.security.Controller.AccountRestController;
import com.example.Event_Management.security.TokenDecoder.JwtTokenDecoder;
import com.example.Event_Management.security.entities.AppUser;
import com.example.Event_Management.security.repository.AppUserRepository;
import com.example.Event_Management.security.services.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.Event_Management.security.Controller.AccountRestController.addAuthButtonAttributes;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
//@RequestMapping("/api/events")
public class EventController {
    private static final Logger logger = LoggerFactory.getLogger(AccountRestController.class);
    private final List list;

    private EventRepository eventRepository;
    private SessionRepository sessionRepository;
    private JwtTokenDecoder jwtTokenDecoder;
    private AccountService accountService;
    private VilleRepository villeRepository;
    private PlaceRepository placeRepository;
    private AppUserRepository appUserRepository;
    private InvitationFormRepository invitationFormRepository;
    private InvitationSessionRepository invitationSessionRepository;

    public EventController(EventRepository eventRepository, SessionRepository sessionRepository, JwtTokenDecoder jwtTokenDecoder, AccountService accountService, VilleRepository villeRepository, PlaceRepository placeRepository, AppUserRepository appUserRepository, InvitationFormRepository invitationFormRepository, InvitationSessionRepository invitationSessionRepository, List list) {
        this.eventRepository = eventRepository;
        this.sessionRepository = sessionRepository;
        this.jwtTokenDecoder = jwtTokenDecoder;
        this.accountService = accountService;
        this.villeRepository = villeRepository;
        this.placeRepository = placeRepository;
        this.appUserRepository = appUserRepository;
        this.invitationFormRepository = invitationFormRepository;
        this.invitationSessionRepository = invitationSessionRepository;
        this.list = list;
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
    public String createEvent(
            @ModelAttribute EventDTO eventDTO,
            Model model, HttpServletRequest request,RedirectAttributes redirectAttributes) {
        // Decode JWT using JwtTokenDecoder
        Map<String, Object> userInfo = jwtTokenDecoder.decodeToken(request);
        model.addAttribute("username", userInfo.get("username"));
        model.addAttribute("roles", userInfo.get("roles"));
        addAuthButtonAttributes(model, userInfo);

        try {
            // Get authenticated user
            AppUser creator = accountService.loadUserByUsername((String) userInfo.get("username"));
            if (creator == null) {
                redirectAttributes.addAttribute("error", "User not found");
                return "redirect:/createEvent";
            }

            // Validate input
            if (eventDTO.getName() == null || eventDTO.getName().trim().isEmpty()) {
                redirectAttributes.addAttribute("error", "Event name is required");
                return "redirect:/createEvent";
            }
            if (eventDTO.getEventType() == null || eventDTO.getEventType().trim().isEmpty()) {
                redirectAttributes.addAttribute("error", "Event type is required");
                return "redirect:/createEvent";
            }
            if (eventDTO.getImage() == null || eventDTO.getImage().isEmpty()) {
                redirectAttributes.addAttribute("error", "Event image is required");
                return "redirect:/createEvent";
            }
            if (eventDTO.getSessions() == null || eventDTO.getSessions().isEmpty()) {
                redirectAttributes.addAttribute("error", "At least one session is required");
                return "redirect:/createEvent";
            }

            // Create and save the event
            Event event = new Event();
            event.setName(eventDTO.getName());
            event.setDescription(eventDTO.getDescription());
            event.setEventType(EventType.valueOf(eventDTO.getEventType()));
            event.setCreatedBy(creator);
            event.setImage(Base64.getEncoder().encodeToString(eventDTO.getImage().getBytes()).getBytes());
            //event.setImage(eventDTO.getImage().getBytes());
            //event.setFacialRecognition(eventDTO.isFacialRecognition());
            event.setCreatorName(creator.getUsername());
            eventRepository.save(event);

            // Create and save sessions
            List<Session> sessions = new ArrayList<>();
            for (int i = 0; i < eventDTO.getSessions().size(); i++) {
                SessionDTO sessionDTO = eventDTO.getSessions().get(i);

                // Validate session data
                if (sessionDTO.getName() == null || sessionDTO.getName().trim().isEmpty() ||
                        sessionDTO.getDate() == null || sessionDTO.getDate().trim().isEmpty() ||
                        sessionDTO.getStartTime() == null || sessionDTO.getStartTime().trim().isEmpty() ||
                        sessionDTO.getEndTime() == null || sessionDTO.getEndTime().trim().isEmpty() ||
                        sessionDTO.getPlace() == null || sessionDTO.getPlace().getName() == null || sessionDTO.getPlace().getName().trim().isEmpty() ||
                        sessionDTO.getPlace().getVille() == null || sessionDTO.getPlace().getVille().getName() == null || sessionDTO.getPlace().getVille().getName().trim().isEmpty()) {
                    redirectAttributes.addAttribute("error", "All session fields are required for session " + (i + 1));
                    return "redirect:/createEvent";
                }

                // Create or reuse Ville
                String villeName = sessionDTO.getPlace().getVille().getName();
                Ville ville = villeRepository.findByName(villeName)
                        .orElseGet(() -> {
                            Ville newVille = new Ville();
                            newVille.setName(villeName);
                            return villeRepository.save(newVille);
                        });

                // Create Place
                Place place = new Place();
                place.setName(sessionDTO.getPlace().getName());
                place.setVille(ville);
                place = placeRepository.save(place);

                // Create Session
                Session session = new Session();
                session.setName(sessionDTO.getName());
                session.setDate(LocalDate.parse(sessionDTO.getDate()));
                session.setStartTime(LocalTime.parse(sessionDTO.getStartTime()));
                session.setEndTime(LocalTime.parse(sessionDTO.getEndTime()));
                session.setEvent(event);
                session.setPlace(place);
                session.setEventName(event.getName());
                session.setCreatorName(creator.getUsername());

                // Process guest speakers for this session
                if (sessionDTO.getGuestSpeakers() != null && !sessionDTO.getGuestSpeakers().trim().isEmpty()) {
                    String[] speakerNames = sessionDTO.getGuestSpeakers().split(",");
                    for (String speakerName : speakerNames) {
                        speakerName = speakerName.trim();
                        if (!speakerName.isEmpty()) {
                            AppUser speaker = appUserRepository.findByUsername(speakerName);
                            if (speaker != null) {
                                session.addGuestSpeaker(speaker, event.getName(), sessionDTO.getName(), speaker.getUsername());
                            } else {
                                redirectAttributes.addAttribute("error", "Speaker not found: " + speakerName);
                                return "redirect:/createEvent";
                            }
                        }
                    }
                }

                sessions.add(session);
            }
            sessionRepository.saveAll(sessions);

            return "redirect:/eventCreated";
        } catch (IOException e) {
            redirectAttributes.addAttribute("error", "Error processing image: " + e.getMessage());
            return "redirect:/createEvent";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addAttribute("error", "Invalid event type or session data: " + e.getMessage());
            return "redirect:/createEvent";
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", "Error creating event: " + e.getMessage());
            return "redirect:/createEvent";
        }
    }



    @GetMapping("/events")
    public String eventsPage(Model model, HttpServletRequest request) {
        // Decode JWT to get user info
        Map<String, Object> userInfo = jwtTokenDecoder.decodeToken(request);
        model.addAttribute("username", userInfo.get("username"));
        model.addAttribute("roles", userInfo.get("roles"));
        addAuthButtonAttributes(model, userInfo);
//
//        // Fetch all events
//        List<Event> events = eventRepository.findAll();
//        List<EventDTO> eventDTOs = new ArrayList<>();
//        for (Event event : events) {
//            EventDTO eventDTO = new EventDTO();
//            eventDTO.setId(event.getId());
//            eventDTO.setName(event.getName());
//            eventDTO.setDescription(event.getDescription());
//            eventDTO.setEventType(event.getEventType() != null ? event.getEventType().toString() : null);
//            //eventDTO.setFacialRecognition(event.isFacialRecognition());
//            eventDTO.setCreatorName(event.getCreatorName());
//
//            // Convert List<Session> to List<SessionDTO>
//            List<SessionDTO> sessionDTOs = new ArrayList<>();
//            for (Session session : event.getSessions()) {
//                SessionDTO sessionDTO = new SessionDTO();
//                sessionDTO.setName(session.getName());
//                sessionDTO.setDate(session.getDate() != null ? session.getDate().toString() : null);
//                sessionDTO.setStartTime(session.getStartTime() != null ? session.getStartTime().toString() : null);
//                sessionDTO.setEndTime(session.getEndTime() != null ? session.getEndTime().toString() : null);
//                if (session.getPlace() != null) {
//                    PlaceDTO placeDTO = new PlaceDTO();
//                    placeDTO.setName(session.getPlace().getName());
//                    if (session.getPlace().getVille() != null) {
//                        VilleDTO villeDTO = new VilleDTO();
//                        villeDTO.setName(session.getPlace().getVille().getName());
//                        placeDTO.setVille(villeDTO);
//                    }
//                    sessionDTO.setPlace(placeDTO);
//                }
//                // Set guest speakers as a comma-separated string
//                if (session.getGuestSpeakers() != null && !session.getGuestSpeakers().isEmpty()) {
//                    String guestSpeakers = session.getGuestSpeakers().stream()
//                            .map(SessionSpeakers::getSpeakerName)
//                            .collect(Collectors.joining(","));
//                    sessionDTO.setGuestSpeakers(guestSpeakers);
//                }
//                sessionDTOs.add(sessionDTO);
//            }
//            eventDTO.setSessions(sessionDTOs);
//
//            if (event.getImage() != null && event.getImage().length > 0) {
//                try {
//                    String base64Image = Base64.getEncoder().encodeToString(event.getImage());
//                    eventDTO.setImageBase64(base64Image);
//                    // Log the size of the encoded image
//                    logger.debug("Encoded image size for event {}: {} bytes", event.getName(), base64Image.length());
//                } catch (Exception e) {
//                    logger.error("Error encoding image for event {}: {}", event.getName(), e.getMessage());
//                }
//            } else {
//                logger.warn("No image data for event {}", event.getName());
//            }
//            eventDTOs.add(eventDTO);
//        }
//        logger.debug("Loaded {} events", eventDTOs.size());
//        model.addAttribute("events", eventDTOs);


        List<Event> events = eventRepository.findAll();
        for(Event event : events) {
            try {
                String base64Image = Base64.getEncoder().encodeToString(event.getImage());
                event.setImageBase64(base64Image);
                // Log the size of the encoded image
                logger.debug("Encoded image size for event {}: {} bytes", event.getName(), base64Image.length());
            } catch (Exception e) {
                logger.error("Error encoding image for event {}: {}", event.getName(), e.getMessage());
            }
        }


        model.addAttribute("events", events);
        return "Events/events";
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



    @GetMapping("/eventCreated")
    public String eventCreated(Model model, HttpServletRequest request) {
        Map<String, Object> userInfo = jwtTokenDecoder.decodeToken(request);
        model.addAttribute("username", userInfo.get("username"));
        model.addAttribute("roles", userInfo.get("roles"));
        addAuthButtonAttributes(model, userInfo);
        return "Events/eventCreated";
    }





    @GetMapping("/register")
    public String registerPage(@ModelAttribute("eventId") Long eventId, Model model, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        logger.debug("Processing register page request for eventId: {}", eventId);

        // Check if user is authenticated
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated();

        if (!isAuthenticated) {
            logger.debug("User is not authenticated, redirecting to /signup with eventId: {}", eventId);
            return "redirect:/signup";
        }

        Map<String, Object> userInfo = jwtTokenDecoder.decodeToken(request);
        model.addAttribute("username", userInfo.get("username"));
        model.addAttribute("roles", userInfo.get("roles"));
        addAuthButtonAttributes(model, userInfo);

        Optional<Event> eventOpt = eventRepository.findById(eventId);
        if (!eventOpt.isPresent()) {
            redirectAttributes.addAttribute("error", "Event not found");
            return "redirect:/events";
        }
        Event event = eventOpt.get();
        model.addAttribute("eventName", event.getName());

        InvitationFormDTO invitationFormDTO = new InvitationFormDTO();
        invitationFormDTO.setEventId(eventId);
        model.addAttribute("invitationFormDTO", invitationFormDTO);

        return "Events/register";
    }

    @PostMapping("/register")
    public String submitRegistration(@Valid @ModelAttribute("invitationFormDTO") InvitationFormDTO invitationFormDTO, BindingResult result, Model model, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        logger.debug("Processing registration submission for eventId: {}", invitationFormDTO.getEventId());
        Map<String, Object> userInfo = jwtTokenDecoder.decodeToken(request);
        model.addAttribute("username", userInfo.get("username"));
        model.addAttribute("roles", userInfo.get("roles"));
        addAuthButtonAttributes(model, userInfo);

        if (result.hasErrors()) {
            Optional<Event> eventOpt = eventRepository.findById(invitationFormDTO.getEventId());
            if (eventOpt.isPresent()) {
                model.addAttribute("eventName", eventOpt.get().getName());
            }
            return "Events/register";
        }

        Optional<Event> eventOpt = eventRepository.findById(invitationFormDTO.getEventId());
        if (!eventOpt.isPresent()) {
            redirectAttributes.addAttribute("error", "Event not found");
            return "redirect:/events";
        }
        Event event = eventOpt.get();
        List<Session> sessions = event.getSessions();
        if (sessions == null || sessions.isEmpty()) {
            redirectAttributes.addAttribute("error", "No sessions available for this event");
            return "redirect:/events";
        }

        InvitationForm invitationForm = new InvitationForm(
                invitationFormDTO.getFirstName(),
                invitationFormDTO.getLastName(),
                invitationFormDTO.getPhoneNumber(),
                invitationFormDTO.getEmail()
        );
        invitationFormRepository.save(invitationForm);

        String invitee = invitationFormDTO.getFirstName() + " " + invitationFormDTO.getLastName();
        for (Session session : sessions) {
            InvitationSession invitationSession = new InvitationSession(
                    invitationForm,
                    session,
                    event.getName(),
                    session.getName(),
                    invitee
            );
            invitationForm.addInvitationSession(invitationSession);
            session.getInvitationSessions().add(invitationSession);
            invitationSessionRepository.save(invitationSession);
        }

        redirectAttributes.addFlashAttribute("success", "Registration successful for " + event.getName());
        return "redirect:/events";
    }

}