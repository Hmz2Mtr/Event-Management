package com.example.Event_Management.security;

import com.example.Event_Management.security.entities.AppUser;
import com.example.Event_Management.security.filters.JwtAuthenticationFilter;
import com.example.Event_Management.security.filters.JwtAuthorizationFilter;
import com.example.Event_Management.security.services.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.ArrayList;
import java.util.Collection;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Autowired
    private AccountService accountService;
    @Bean
    public UserDetailsService userDetailsService() {
        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                AppUser appUser = accountService.loadUserByUsername(username);
                if (appUser == null) {
                    throw new UsernameNotFoundException("User not found");
                }
                Collection<GrantedAuthority> authorities = new ArrayList<>();
                appUser.getAppRoles().forEach(role ->
                        authorities.add(new SimpleGrantedAuthority(role.getRoleName()))
                );
                return new User(appUser.getUsername(), appUser.getPassword(), authorities);
            }
        };
    }

    @Bean
    protected SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/assets/**",
                                "/error",
                                "/h2-console/**",
                                "/refreshToken/**",
                                "/events/**",
                                "/",
                                "/signin/**",
                                "/signup/**",
                                "/about",
                                "/pricing",
                                "/contact",
                                "/scan/**",
                                "/eventDetails",
                                "/createEvent/**",
                                "/eventss/**",
                                "/index2/**",
                                "/recognize/**",
                                "/store/**",
                                "/logout").permitAll()
                        .requestMatchers("GET", "/createEvent/**").permitAll() // Allow GET /events for all
                        .requestMatchers("POST", "/createEvent/**").hasAnyAuthority("ADMIN", "SUPER_ADMIN") // Restrict POST /events
                        .anyRequest().authenticated()
                )
//                .formLogin(form -> form
//                        .loginPage("/login") // Specify the custom login page
//                        .defaultSuccessUrl("/home", true) // Redirect after successful login
//                        .failureUrl("/login?error=true") // Redirect on login failure
//                        .permitAll()
//                )

                .formLogin(form -> form.disable()) // Disable default form login
                .logout(logout -> logout.disable()) // Disable default logout handling
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/signin/**","/signup/**") // Disable CSRF for /signin
                        .ignoringRequestMatchers("/h2-console/**")  // Only disable CSRF for H2 consol
                        .ignoringRequestMatchers("/logout")  // Only disable CSRF for H2 console
                        //.disable()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // Enable session management
                )
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.disable())
                )
                .addFilter(new JwtAuthenticationFilter(authenticationManagerBean(http.getSharedObject(AuthenticationConfiguration.class))))
                .addFilterBefore(new JwtAuthorizationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManagerBean(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
