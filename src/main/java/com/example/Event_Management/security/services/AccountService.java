package com.example.Event_Management.security.services;

import com.example.Event_Management.security.entities.AppRole;
import com.example.Event_Management.security.entities.AppUser;

import java.util.List;

public interface AccountService {
    AppUser addNewUser (AppUser appUser);
    AppRole addNewRole(AppRole appRole);
    void addRoleToUser(String username, String roleName);
    AppUser loadUserByUsername(String username);
    List<AppUser> listUsers();

    void updateUser(AppUser user);

    void updateUsersPassword(AppUser user);
}
