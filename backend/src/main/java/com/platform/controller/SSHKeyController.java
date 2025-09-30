// src/main/java/com/platform/controller/SSHKeyController.java
package com.platform.controller;

import com.platform.dto.request.AddSSHKeyRequest;
import com.platform.dto.response.MessageResponse;
import com.platform.dto.response.SSHKeyDTO;
import com.platform.security.jwt.UserPrincipal;
import com.platform.service.SSHKeyService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ssh-keys")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SSHKeyController {

    private static final Logger logger = LoggerFactory.getLogger(SSHKeyController.class);

    @Autowired
    private SSHKeyService sshKeyService;

    @PostMapping
    public ResponseEntity<SSHKeyDTO> addSSHKey(
            @Valid @RequestBody AddSSHKeyRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.info("Adding SSH key '{}' for user: {}", request.getName(), userPrincipal.getUsername());
        
        SSHKeyDTO sshKey = sshKeyService.addSSHKey(request, userPrincipal.getUsername());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(sshKey);
    }

    @GetMapping
    public ResponseEntity<List<SSHKeyDTO>> getUserSSHKeys(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.debug("Fetching SSH keys for user: {}", userPrincipal.getUsername());
        
        List<SSHKeyDTO> sshKeys = sshKeyService.getUserSSHKeys(userPrincipal.getUsername());
        
        return ResponseEntity.ok(sshKeys);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteSSHKey(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.info("Deleting SSH key {} for user: {}", id, userPrincipal.getUsername());
        
        sshKeyService.deleteSSHKey(id, userPrincipal.getUsername());
        
        return ResponseEntity.ok(MessageResponse.success("SSH key deleted successfully"));
    }

    @GetMapping("/stats")
    public ResponseEntity<SSHKeyStatsResponse> getUserSSHKeyStats(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        List<SSHKeyDTO> sshKeys = sshKeyService.getUserSSHKeys(userPrincipal.getUsername());
        
        long totalKeys = sshKeys.size();
        long activeKeys = sshKeys.stream().filter(SSHKeyDTO::isActive).count();
        long inactiveKeys = totalKeys - activeKeys;
        
        SSHKeyStatsResponse stats = new SSHKeyStatsResponse(totalKeys, activeKeys, inactiveKeys);
        
        return ResponseEntity.ok(stats);
    }

    // Inner class for stats response
    public static class SSHKeyStatsResponse {
        private long totalKeys;
        private long activeKeys;
        private long inactiveKeys;

        public SSHKeyStatsResponse(long totalKeys, long activeKeys, long inactiveKeys) {
            this.totalKeys = totalKeys;
            this.activeKeys = activeKeys;
            this.inactiveKeys = inactiveKeys;
        }

        public long getTotalKeys() { return totalKeys; }
        public void setTotalKeys(long totalKeys) { this.totalKeys = totalKeys; }

        public long getActiveKeys() { return activeKeys; }
        public void setActiveKeys(long activeKeys) { this.activeKeys = activeKeys; }

        public long getInactiveKeys() { return inactiveKeys; }
        public void setInactiveKeys(long inactiveKeys) { this.inactiveKeys = inactiveKeys; }
    }
}
