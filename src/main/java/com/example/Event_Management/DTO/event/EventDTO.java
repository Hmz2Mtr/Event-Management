package com.example.Event_Management.DTO.event;
import com.example.Event_Management.entities.event.Session;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventDTO {
    private Long id;
    private String name;
    private String description;
    private String eventType;
    private MultipartFile image;
    private String imageBase64;
    private boolean facialRecognition;
    private String creatorName;
    private List<SessionDTO> sessions;
}