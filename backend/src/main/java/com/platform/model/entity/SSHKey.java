// src/main/java/com/platform/model/entity/SSHKey.java
package com.platform.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "ssh_keys")
@EntityListeners(AuditingEntityListener.class)
public class SSHKey {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Column(name = "public_key", columnDefinition = "TEXT", nullable = false)
    private String publicKey;

    @Column(name = "fingerprint", unique = true)
    private String fingerprint;

    @Column(name = "key_type")
    private String keyType; // rsa, ed25519, ecdsa

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_used")
    private LocalDateTime lastUsed;

    // Constructors
    public SSHKey() {}

    public SSHKey(String name, String publicKey, User user) {
        this.name = name;
        this.publicKey = publicKey;
        this.user = user;
        this.fingerprint = generateFingerprint(publicKey);
        this.keyType = extractKeyType(publicKey);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { 
        this.publicKey = publicKey;
        this.fingerprint = generateFingerprint(publicKey);
        this.keyType = extractKeyType(publicKey);
    }

    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }

    public String getKeyType() { return keyType; }
    public void setKeyType(String keyType) { this.keyType = keyType; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastUsed() { return lastUsed; }
    public void setLastUsed(LocalDateTime lastUsed) { this.lastUsed = lastUsed; }

    // Utility methods
    private String generateFingerprint(String publicKey) {
        if (publicKey == null || publicKey.trim().isEmpty()) {
            return null;
        }
        // Simplified fingerprint generation - in production use proper SSH key parsing
        return "SHA256:" + Integer.toHexString(publicKey.hashCode());
    }

    private String extractKeyType(String publicKey) {
        if (publicKey == null) return null;
        
        if (publicKey.startsWith("ssh-rsa")) return "rsa";
        if (publicKey.startsWith("ssh-ed25519")) return "ed25519";
        if (publicKey.startsWith("ecdsa-sha2")) return "ecdsa";
        return "unknown";
    }

    public void updateLastUsed() {
        this.lastUsed = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "SSHKey{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", keyType='" + keyType + '\'' +
                ", fingerprint='" + fingerprint + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}