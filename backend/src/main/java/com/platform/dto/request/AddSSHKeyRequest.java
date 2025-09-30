// src/main/java/com/platform/dto/request/AddSSHKeyRequest.java
package com.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AddSSHKeyRequest {
    
    @NotBlank(message = "SSH key name is required")
    @Size(min = 1, max = 100, message = "SSH key name must be between 1 and 100 characters")
    private String name;
    
    @NotBlank(message = "Public key is required")
    @Size(max = 4096, message = "Public key is too long")
    private String publicKey;
    
    public AddSSHKeyRequest() {}
    
    public AddSSHKeyRequest(String name, String publicKey) {
        this.name = name;
        this.publicKey = publicKey;
    }
    
    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
}