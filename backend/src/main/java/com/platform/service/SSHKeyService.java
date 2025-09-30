// src/main/java/com/platform/service/SSHKeyService.java
package com.platform.service;

import com.platform.dto.request.AddSSHKeyRequest;
import com.platform.dto.response.SSHKeyDTO;
import com.platform.exception.BadRequestException;
import com.platform.exception.ResourceNotFoundException;
import com.platform.model.entity.SSHKey;
import com.platform.model.entity.User;
import com.platform.repository.SSHKeyRepository;
import com.platform.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SSHKeyService {

    private static final Logger logger = LoggerFactory.getLogger(SSHKeyService.class);
    private static final int MAX_KEYS_PER_USER = 5;

    @Autowired
    private SSHKeyRepository sshKeyRepository;

    @Autowired
    private UserRepository userRepository;

    public SSHKeyDTO addSSHKey(AddSSHKeyRequest request, String username) {
        // Find user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check SSH key limit
        long keyCount = sshKeyRepository.countByUser(user);
        if (keyCount >= MAX_KEYS_PER_USER) {
            throw new BadRequestException("SSH key limit exceeded (max " + MAX_KEYS_PER_USER + " keys per user)");
        }

        // Validate public key format
        validatePublicKey(request.getPublicKey());

        // Check for duplicate key
        if (sshKeyRepository.existsByUserAndPublicKey(user, request.getPublicKey())) {
            throw new BadRequestException("SSH key already exists");
        }

        // Create and save SSH key
        SSHKey sshKey = new SSHKey();
        sshKey.setName(request.getName());
        sshKey.setPublicKey(request.getPublicKey());
        sshKey.setUser(user);
        sshKey.setEnabled(true);

        sshKey = sshKeyRepository.save(sshKey);

        logger.info("SSH key '{}' added for user: {}", request.getName(), username);

        return SSHKeyDTO.fromSSHKey(sshKey);
    }

    public List<SSHKeyDTO> getUserSSHKeys(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<SSHKey> keys = sshKeyRepository.findByUserOrderByCreatedAtDesc(user);

        return keys.stream()
                .map(SSHKeyDTO::fromSSHKey)
                .collect(Collectors.toList());
    }

    public void deleteSSHKey(Long keyId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SSHKey sshKey = sshKeyRepository.findByIdAndUser(keyId, user)
                .orElseThrow(() -> new ResourceNotFoundException("SSH key not found"));

        sshKeyRepository.delete(sshKey);

        logger.info("SSH key '{}' deleted for user: {}", sshKey.getName(), username);
    }

    public void validatePublicKey(String publicKey) {
        if (publicKey == null || publicKey.trim().isEmpty()) {
            throw new BadRequestException("Public key cannot be empty");
        }

        // Basic SSH public key format validation
        String trimmedKey = publicKey.trim();
        
        // Check if key starts with known SSH key types
        if (!trimmedKey.startsWith("ssh-rsa ") && 
            !trimmedKey.startsWith("ssh-ed25519 ") && 
            !trimmedKey.startsWith("ecdsa-sha2-")) {
            throw new BadRequestException("Invalid SSH public key format");
        }

        // Basic structure validation (type + base64 data + optional comment)
        String[] parts = trimmedKey.split("\\s+");
        if (parts.length < 2) {
            throw new BadRequestException("Invalid SSH public key format");
        }

        // Check if base64 part looks valid (basic check)
        String base64Part = parts[1];
        if (base64Part.length() < 50) { // SSH keys are typically much longer
            throw new BadRequestException("Invalid SSH public key format");
        }
    }

    // Method to find SSH key by public key (for authentication)
    public SSHKey findByPublicKey(String publicKey) {
        return sshKeyRepository.findByPublicKeyAndEnabled(publicKey)
                .orElse(null);
    }

    // Method to update last used timestamp
    public void updateLastUsed(Long keyId) {
        sshKeyRepository.findById(keyId).ifPresent(key -> {
            key.updateLastUsed();
            sshKeyRepository.save(key);
        });
    }
}