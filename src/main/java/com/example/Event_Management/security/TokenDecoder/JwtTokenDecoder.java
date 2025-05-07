package com.example.Event_Management.security.TokenDecoder;


import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtTokenDecoder {
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenDecoder.class);

    public Map<String, Object> decodeToken(HttpServletRequest request) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", "Guest");
        userInfo.put("roles", Collections.emptyList());

        // Get JWT from access_token cookie
        String jwtToken = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    jwtToken = cookie.getValue();
                    logger.debug("Found access_token cookie: {}", jwtToken);
                    break;
                }
            }
        } else {
            logger.debug("No cookies found in request");
        }

        if (jwtToken != null) {
            try {
                // Decode JWT
                DecodedJWT decodedJWT = JWT.decode(jwtToken);
                String username = decodedJWT.getSubject();
                List<String> roles = decodedJWT.getClaim("roles").asList(String.class);
                logger.debug("JWT decoded: username={}, roles={}", username, roles);

                // Update userInfo with decoded values
                userInfo.put("username", username != null ? username : "Guest");
                userInfo.put("roles", roles != null ? roles : Collections.emptyList());
            } catch (JWTDecodeException e) {
                logger.error("Failed to decode JWT: {}", e.getMessage(), e);
            }
        } else {
            logger.debug("No JWT token found, returning default user info");
        }

        return userInfo;
    }
}