package com.example.Event_Management.security.services;

import com.example.Event_Management.security.entities.AppRole;
import com.example.Event_Management.security.entities.AppUser;
import com.example.Event_Management.security.repository.AppRoleRepository;
import com.example.Event_Management.security.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AccountServiceImpl implements AccountService {
    private AppUserRepository appUserRepository;
    private AppRoleRepository appRoleRepository;

    private PasswordEncoder passwordEncoder;

    public AccountServiceImpl(AppUserRepository appUserRepository, AppRoleRepository appRoleRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.appRoleRepository = appRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public AppUser addNewUser(AppUser appUser) {
        String pwd = appUser.getPassword();
        appUser.setPassword(passwordEncoder.encode(pwd));
        return appUserRepository.save(appUser);
    }

    @Override
    public AppRole addNewRole(AppRole appRole) {
        return appRoleRepository.save(appRole);
    }

    @Override
    public void addRoleToUser(String username, String roleName) {
        AppUser appUser = appUserRepository.findByUsername(username);
        AppRole appRole = appRoleRepository.findByRoleName(roleName);
        if (appUser == null) {
            throw new RuntimeException("User not found");
        }else if (appRole == null) {
            throw new RuntimeException("Role not found");
        }
        appUser.getAppRoles().add(appRole);
    }

    @Override
    public AppUser loadUserByUsername(String username) {
        return appUserRepository.findByUsername(username);
    }

    @Override
    public List<AppUser> listUsers() {
        return appUserRepository.findAll();
    }

    @Override
    public void updateUser(AppUser user) {

        AppUser existingUser = appUserRepository.findById(user.getId()).orElseThrow(() -> new RuntimeException("User not found"));
        existingUser.setUsername(user.getUsername());
        existingUser.setEmail(user.getEmail());
        existingUser.setFirstName(user.getFirstName());
        existingUser.setLastName(user.getLastName());
        existingUser.setNumberPhone(user.getNumberPhone());
        existingUser.setAppRoles(existingUser.getAppRoles());
        existingUser.setProfilePicture(existingUser.getProfilePicture());
        existingUser.setPassword(existingUser.getPassword());
        appUserRepository.save(existingUser);
    }

    @Override
    public void updateUsersPassword(AppUser user) {
        AppUser existingUser = appUserRepository.findById(user.getId()).orElseThrow(() -> new RuntimeException("User not found"));
        existingUser.setPassword(user.getPassword());
        appUserRepository.save(existingUser);
    }
}
