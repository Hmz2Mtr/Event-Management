package com.example.Event_Management.security.repository;

import com.example.Event_Management.security.entities.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    AppUser findByUsername(String username);

    List<AppUser> findByUsernameIn(List<String> inviteeUsernames);
}
