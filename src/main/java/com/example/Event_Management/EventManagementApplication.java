package com.example.Event_Management;

import com.example.Event_Management.security.entities.AppRole;
import com.example.Event_Management.security.entities.AppUser;
import com.example.Event_Management.security.repository.AppRoleRepository;
import com.example.Event_Management.security.repository.AppUserRepository;
import com.example.Event_Management.security.services.AccountService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.hateoas.server.core.DelegatingLinkRelationProvider;
import org.springframework.security.config.annotation.authentication.configuration.EnableGlobalAuthentication;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;

@SpringBootApplication
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class EventManagementApplication implements CommandLineRunner{

	public static void main(String[] args) {
		SpringApplication.run(EventManagementApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		System.out.println("Hello!!!!!");
	}
	@Bean
	PasswordEncoder passwordEncoder(){
		return new BCryptPasswordEncoder();
	}

	@Bean
	CommandLineRunner start(AccountService accountService, AppUserRepository userRepository, AppRoleRepository roleRepository) {
		return args -> {
			// Clear existing data (temporary)
			userRepository.deleteAll();
			roleRepository.deleteAll();

			// Add roles
			accountService.addNewRole(new AppRole(null, "SUPER_ADMIN"));
			accountService.addNewRole(new AppRole(null, "ADMIN"));
			accountService.addNewRole(new AppRole(null, "USER"));

			// Add users
			accountService.addNewUser(new AppUser(null, "ana", "1234",new ArrayList<>(), null, "ana.ana@gmail.com", "ana", "anitoo", "0625242829"));
			accountService.addNewUser(new AppUser(null, "admin", "1234", new ArrayList<>(), null,"admin.ana@gmail.com", "admin", "adimtoo", "0625242829"));
			accountService.addNewUser(new AppUser(null, "user1", "1234", new ArrayList<>(), null,"user1.ana@gmail.com", "user1", "usirtoo", "0625242829"));

			accountService.addRoleToUser("ana", "SUPER_ADMIN");
			accountService.addRoleToUser("ana", "ADMIN");
			accountService.addRoleToUser("ana", "USER");
			accountService.addRoleToUser("admin", "ADMIN");
			accountService.addRoleToUser("admin", "USER");
			accountService.addRoleToUser("user1", "USER");
		};
	}
}
