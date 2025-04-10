package com.example.Event_Management;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/")
    public String home() {
        return "home"; // This corresponds to a `home.html` file in the templates folder
    }
}
