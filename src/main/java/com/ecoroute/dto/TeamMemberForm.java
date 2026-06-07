package com.ecoroute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class TeamMemberForm {

    @NotBlank(message = "Username is required.")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters.")
    private String username;

    @NotBlank(message = "Email is required.")
    @Pattern(
        regexp = "^[A-Za-z0-9._%+-]+@(gmail\\.com|[A-Za-z0-9.-]+\\.[A-Za-z]{2,})$",
        message = "Email must be a valid address ending with @gmail.com or a registered corporate domain."
    )
    private String email;

    @NotBlank(message = "Password is required.")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,30}$",
        message = "Password must be 8-30 characters long and include at least one uppercase letter, one lowercase letter, one number, and one special character."
    )
    private String password;

    @NotBlank(message = "Role is required.")
    private String roleStr;

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRoleStr() {
        return roleStr;
    }

    public void setRoleStr(String roleStr) {
        this.roleStr = roleStr;
    }
}
