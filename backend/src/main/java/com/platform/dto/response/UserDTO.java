// src/main/java/com/platform/dto/response/UserDTO.java
package com.platform.dto.response;

import com.platform.model.entity.User;
import com.platform.model.enums.UserRole;

import java.time.LocalDateTime;

public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private UserRole role;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int containerCount;
    private int sshKeyCount;

    public UserDTO() {}

    public UserDTO(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.role = user.getRole();
        this.enabled = user.getEnabled();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
        this.containerCount = user.getContainers().size();
        this.sshKeyCount = user.getSshKeys().size();
    }

    // Static factory method
    public static UserDTO fromUser(User user) {
        return new UserDTO(user);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public int getContainerCount() { return containerCount; }
    public void setContainerCount(int containerCount) { this.containerCount = containerCount; }

    public int getSshKeyCount() { return sshKeyCount; }
    public void setSshKeyCount(int sshKeyCount) { this.sshKeyCount = sshKeyCount; }

    // Utility methods
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        return username;
    }

    public AuthResponse.UserInfo toUserInfo() {
        return new AuthResponse.UserInfo(
                this.id,
                this.username,
                this.email,
                this.firstName,
                this.lastName,
                this.role.name(),
                this.createdAt
        );
    }
}