package com.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;

public class CreateContainerRequest {
    
    @NotBlank(message = "Container name is required")
    @Size(min = 3, max = 50, message = "Container name must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9_-]*$", message = "Container name must start with alphanumeric character and contain only letters, numbers, hyphens, and underscores")
    private String name;
    
    @NotBlank(message = "Image is required")
    private String image;
    
    private Map<String, String> environmentVariables;

    // Constructors
    public CreateContainerRequest() {}

    public CreateContainerRequest(String name, String image) {
        this.name = name;
        this.image = image;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public Map<String, String> getEnvironmentVariables() { return environmentVariables; }
    public void setEnvironmentVariables(Map<String, String> environmentVariables) { 
        this.environmentVariables = environmentVariables; 
    }
}