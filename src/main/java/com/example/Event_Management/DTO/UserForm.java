package com.example.Event_Management.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserForm {
    @NotBlank
    private String username;
    @NotBlank
    @Email
    private String email;

    private String firstName;
    private String lastName;
    private String numberPhone;
    private String profilePicture;

//    public UserForm(String username, String email, String numberPhone) {
//        this.username = username;
//        this.email = email;
//        this.numberPhone = numberPhone;
//    }


    public UserForm(String username, String email, String firstName, String lastName, String numberPhone) {
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.numberPhone = numberPhone;
    }
}
