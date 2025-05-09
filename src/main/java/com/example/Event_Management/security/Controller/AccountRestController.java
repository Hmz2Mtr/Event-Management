package com.example.Event_Management.security.Controller;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.Event_Management.security.JWTUtil;
import com.example.Event_Management.security.entities.AppRole;
import com.example.Event_Management.security.entities.AppUser;
import com.example.Event_Management.security.services.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;
import java.security.Principal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.Event_Management.security.TokenDecoder.JwtTokenDecoder;


@Controller
public class AccountRestController {
    private static final Logger logger = LoggerFactory.getLogger(AccountRestController.class);
    private AccountService accountService;
    private final AuthenticationManager authenticationManager;
    private JwtTokenDecoder jwtTokenDecoder;
    private PasswordEncoder passwordEncoder;

    public AccountRestController (AccountService accountService, AuthenticationManager authenticationManager, PasswordEncoder passwordEncoder, JwtTokenDecoder jwtTokenDecoder, PasswordEncoder passwordEncoder1) {
        this.accountService = accountService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenDecoder = jwtTokenDecoder;
        this.passwordEncoder = passwordEncoder1;
    }


    @GetMapping("/signin")
    public String login() {
        logger.debug("Rendering login page");
        return "security/login";
    }
    @PostMapping(value = "/signin", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> signin(@RequestParam("username") String username, @RequestParam("password") String password) {
        logger.debug("Received signin request: username={}", username);

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            logger.warn("Missing or empty username or password");
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Username and password are required");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        try {
            logger.debug("Authenticating user: {}", username);
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            logger.debug("Generating JWT for user: {}", username);
            String jwtToken = JWT.create()
                    .withSubject(username)
                    .withExpiresAt(new Date(System.currentTimeMillis() + JWTUtil.EXPIRE_ACCESS_TOKEN))
                    .withClaim("roles", authentication.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toList()))
                    .sign(Algorithm.HMAC256(JWTUtil.SECRET));

            // Set JWT token in a secure cookie
            String cookie = String.format("access_token=%s; Path=/; HttpOnly; SameSite=Strict", jwtToken);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.SET_COOKIE, cookie);
            headers.setLocation(URI.create("/")); // Redirect to home page

            return ResponseEntity
                    .status(302) // HTTP 302 Found for redirect
                    .headers(headers)
                    .build();
        } catch (Exception e) {
            logger.error("Authentication failed for user {}: {}", username, e.getMessage(), e);
            // Redirect back to login page with error parameter
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create("/signin?error"));
            return ResponseEntity
                    .status(302)
                    .headers(headers)
                    .build();
        }
    }
    @GetMapping("/signup")
    public String signup(Model model, HttpServletRequest request) {
        logger.debug("Rendering signup page");
        Map<String, Object> userInfo = jwtTokenDecoder.decodeToken(request);
        addAuthButtonAttributes(model, userInfo);
        return "security/signup";
    }
    @PostMapping(value = "/signup")
    public ResponseEntity<?> signup(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model) {
        logger.debug("Received signup request: username={}", username);

        if (username == null || username.isEmpty() || password == null || password.isEmpty() || confirmPassword == null || confirmPassword.isEmpty()) {
            logger.warn("Missing or empty username, password, or confirm password");
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create("/signup?error=Missing%20required%20fields"));
            return ResponseEntity.status(302).headers(headers).build();
        }
        if (!password.equals(confirmPassword)) {
            logger.warn("Passwords do not match for username: {}", username);
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create("/signup?error=Passwords%20do%20not%20match"));
            return ResponseEntity.status(302).headers(headers).build();
        }

        try {
            // Check if username is already taken
            if (accountService.loadUserByUsername(username) != null) {
                logger.warn("Username already exists: {}", username);
                HttpHeaders headers = new HttpHeaders();
                headers.setLocation(URI.create("/signup?error=Username%20already%20taken"));
                return ResponseEntity.status(302).headers(headers).build();
            }
            // Create new user
            AppUser newUser = new AppUser();
            newUser.setUsername(username);
            newUser.setPassword(password);
            accountService.addNewUser(newUser);

            // Assign default USER role
            accountService.addRoleToUser(username, "USER");

            logger.debug("User created successfully: {}", username);
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create("/signin?success"));
            return ResponseEntity.status(302).headers(headers).build();
        } catch (Exception e) {
            logger.error("Failed to create user {}: {}", username, e.getMessage(), e);
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create("/signup?error=Failed%20to%20create%20user"));
            return ResponseEntity.status(302).headers(headers).build();
        }
    }




    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        logger.debug("Processing logout request");
        String cookie = "access_token=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0";
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie);
        headers.setLocation(URI.create("/"));
        return ResponseEntity
                .status(302)
                .headers(headers)
                .build();
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////
///
///
////////////////////////////////////////////////////////////////////////////////////////////////////////

    @GetMapping("/")
    public String index(Model model, HttpServletRequest request) {
        logger.debug("Processing home page request");

        // Decode JWT using JwtTokenDecoder
        Map<String, Object> userInfo = jwtTokenDecoder.decodeToken(request);
        model.addAttribute("username", userInfo.get("username"));
        model.addAttribute("roles", userInfo.get("roles"));

        addAuthButtonAttributes(model, userInfo);

        return "Home/index";
    }

//    @GetMapping("/events")
//    public String events(Model model, HttpServletRequest request) {
//        logger.debug("Processing home page request");
//
//        // Decode JWT using JwtTokenDecoder
//        Map<String, Object> userInfo = jwtTokenDecoder.decodeToken(request);
//        model.addAttribute("username", userInfo.get("username"));
//        model.addAttribute("roles", userInfo.get("roles"));
//
//        addAuthButtonAttributes(model, userInfo);
//        return "Home/events";
//    }

    @GetMapping("/about")
    public String adbout(Model model, HttpServletRequest request) {
        logger.debug("Processing home page request");

        // Decode JWT using JwtTokenDecoder
        Map<String, Object> userInfo = jwtTokenDecoder.decodeToken(request);
        model.addAttribute("username", userInfo.get("username"));
        model.addAttribute("roles", userInfo.get("roles"));

        addAuthButtonAttributes(model, userInfo);return "Home/about";
    }

    @GetMapping("/contact")
    public String contact(Model model, HttpServletRequest request) {
        // Decode JWT using JwtTokenDecoder
        Map<String, Object> userInfo = jwtTokenDecoder.decodeToken(request);
        model.addAttribute("username", userInfo.get("username"));
        model.addAttribute("roles", userInfo.get("roles"));

        addAuthButtonAttributes(model, userInfo);
        return "Home/contact";
    }

    @GetMapping("/scan")
    public String scan(Model model, HttpServletRequest request) {
        logger.debug("Processing home page request");

        // Decode JWT using JwtTokenDecoder
        Map<String, Object> userInfo = jwtTokenDecoder.decodeToken(request);
        model.addAttribute("username", userInfo.get("username"));
        model.addAttribute("roles", userInfo.get("roles"));

        addAuthButtonAttributes(model, userInfo);
        return "Home/scan";
    }

    @GetMapping("/pricing")
    public String pricing(Model model, HttpServletRequest request) {
        logger.debug("Processing home page request");

        // Decode JWT using JwtTokenDecoder
        Map<String, Object> userInfo = jwtTokenDecoder.decodeToken(request);
        model.addAttribute("username", userInfo.get("username"));
        model.addAttribute("roles", userInfo.get("roles"));

        addAuthButtonAttributes(model, userInfo);
        return "Home/pricing";
    }






    @GetMapping(path = "/users")
    @PostAuthorize("hasAuthority('USER')")
    public List<AppUser> appUsers() {
        return accountService.listUsers();
    }
    @PostMapping(path = "/users")
    @PostAuthorize("hasAuthority('ADMIN') or hasAuthority('SUPER_ADMIN')")
    public AppUser saveUsers(@RequestBody AppUser appUser) {
        return accountService.addNewUser(appUser);
    }


    @PostMapping(path = "/roles")
    public AppRole saveRole(@RequestBody AppRole appRole) {
        return accountService.addNewRole(appRole);
    }
    @PostMapping(path = "/addRoleToUser")
    public void addRoleToUser(@RequestBody RoleUserForm roleUserForm) {
        accountService.addRoleToUser(roleUserForm.getUsername(), roleUserForm.getRoleName());
    }



    @GetMapping(path = "/profile")
    public AppUser profile(Principal principal){
        return accountService.loadUserByUsername(principal.getName());
    }



















    @GetMapping(path = "/refreshToken")
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) throws Exception{
        String authorizationHeader = request.getHeader(JWTUtil.AUTH_HEADER);
        if (authorizationHeader != null || authorizationHeader.startsWith(JWTUtil.PREFIX)) {
            try{
                String refreshToken = authorizationHeader.substring(JWTUtil.PREFIX.length());
                Algorithm algorithm = Algorithm.HMAC256(JWTUtil.SECRET);
                JWTVerifier jwtVerifier = JWT.require(algorithm).build();
                DecodedJWT decodedJWT = jwtVerifier.verify(refreshToken);
                String username = decodedJWT.getSubject();
                AppUser appUser = accountService.loadUserByUsername(username);
                String jwtAccessToken = JWT.create()
                        .withSubject(appUser.getUsername())
                        .withExpiresAt(new Date(System.currentTimeMillis() + JWTUtil.EXPIRE_ACCESS_TOKEN))
                        .withIssuer(request.getRequestURL().toString())
                        .withClaim("roles", appUser.getAppRoles().stream().map(AppRole::getRoleName).collect(Collectors.toList()))
                        .sign(algorithm);
                Map<String, Object> idToken = new HashMap<>();
                idToken.put("access_token", jwtAccessToken);
                idToken.put("refresh_token", refreshToken);
                response.setHeader(JWTUtil.AUTH_HEADER, jwtAccessToken);
                response.setContentType("application/json");
                new ObjectMapper().writeValue(response.getOutputStream(), idToken);
            }catch (Exception e) {
                throw e;
            }
        }else {
            throw new RuntimeException("Refresh token required");
        }


    }


    public static void addAuthButtonAttributes(Model model, Map<String, Object> userInfo) {
        boolean isAuthenticated = !"Guest".equals(userInfo.get("username"));
        model.addAttribute("buttonText", isAuthenticated ? "Sign Out" : "Sign In");
        model.addAttribute("buttonAction", isAuthenticated ? "/logout" : "/signin");
        model.addAttribute("buttonMethod", isAuthenticated ? "post" : "get");
    }

}






@Data
class RoleUserForm{
    private String roleName;
    private String username;
}

@Data
class LoginRequest {
    private String username;
    private String password;
}