package com.example.Event_Management.security.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String username;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    @ManyToMany(fetch = FetchType.EAGER)
    private Collection<AppRole> appRoles= new ArrayList<>();

    @Column(columnDefinition = "TEXT", nullable = true)
    private String profilePicture; // Base64-encoded image

    private String email;

    public AppUser(Long id, String username, String password, Collection<AppRole> appRoles, String email) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.appRoles = appRoles;
        this.email = email;
    }
}