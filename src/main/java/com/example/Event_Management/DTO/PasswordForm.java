package com.example.Event_Management.DTO;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordForm {
    @NotBlank
    private String currentPassword;
    @NotBlank
    private String newPassword;
    @NotBlank
    private String confirmPassword;


}