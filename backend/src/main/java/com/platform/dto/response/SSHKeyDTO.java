// src/main/java/com/platform/dto/response/SSHKeyDTO.java
package com.platform.dto.response;

import com.platform.model.entity.SSHKey;

import java.time.LocalDateTime;

public class SSHKeyDTO {
    private Long id;
    private String name;
    private String publicKeyFingerprint;
    private String publicKeyType;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    
    public SSHKeyDTO() {}
    
    public static SSHKeyDTO fromSSHKey(SSHKey sshKey) {
        SSHKeyDTO dto = new SSHKeyDTO();
        dto.setId(sshKey.getId());
        dto.setName(sshKey.getName());
        dto.setPublicKeyFingerprint(sshKey.getFingerprint());
        dto.setPublicKeyType(sshKey.getKeyType());
        dto.setActive(sshKey.getEnabled() != null ? sshKey.getEnabled() : true);
        dto.setCreatedAt(sshKey.getCreatedAt());
        dto.setLastUsedAt(sshKey.getLastUsed());
        return dto;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getPublicKeyFingerprint() { return publicKeyFingerprint; }
    public void setPublicKeyFingerprint(String publicKeyFingerprint) { 
        this.publicKeyFingerprint = publicKeyFingerprint; 
    }
    
    public String getPublicKeyType() { return publicKeyType; }
    public void setPublicKeyType(String publicKeyType) { this.publicKeyType = publicKeyType; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
}