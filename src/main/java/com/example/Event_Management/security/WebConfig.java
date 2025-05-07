package com.example.Event_Management.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/signin/**")
                .allowedOrigins("http://localhost:8080") // Adjust to your frontend URL
                .allowedMethods("GET", "POST")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}