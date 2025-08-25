// src/main/java/com/platform/controller/AuthController.java
package com.platform.controller;

import com.platform.dto.request.LoginRequest;
import com.platform.dto.request.RefreshTokenRequest;
import com.platform.dto.request.RegisterRequest;
import com.platform.dto.response.AuthResponse;
import com.platform.dto.response.MessageResponse;
import com.platform.dto.response.UserDTO;
import com.platform.security.jwt.UserPrincipal;
import com.platform.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("Login attempt for user: {}", loginRequest.getUsername());
        
        AuthResponse authResponse = authService.login(loginRequest);
        
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        logger.info("Registration attempt for username: {}, email: {}", 
                   registerRequest.getUsername(), registerRequest.getEmail());
        
        AuthResponse authResponse = authService.register(registerRequest);
        
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        logger.debug("Token refresh requested");
        
        AuthResponse authResponse = authService.refreshToken(refreshTokenRequest.getRefreshToken());
        
        return ResponseEntity.ok(authResponse);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        logger.debug("Current user info requested by: {}", userPrincipal.getUsername());
        
        UserDTO userDTO = authService.getCurrentUser(userPrincipal.getUsername());
        
        return ResponseEntity.ok(userDTO);
    }

    @GetMapping("/check-username")
    public ResponseEntity<MessageResponse> checkUsername(@RequestParam String username) {
        boolean available = authService.isUsernameAvailable(username);
        
        if (available) {
            return ResponseEntity.ok(MessageResponse.success("Username is available"));
        } else {
            return ResponseEntity.ok(MessageResponse.error("Username is already taken"));
        }
    }

    @GetMapping("/check-email")
    public ResponseEntity<MessageResponse> checkEmail(@RequestParam String email) {
        boolean available = authService.isEmailAvailable(email);
        
        if (available) {
            return ResponseEntity.ok(MessageResponse.success("Email is available"));
        } else {
            return ResponseEntity.ok(MessageResponse.error("Email is already in use"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        // With JWT, logout is handled client-side by removing the token
        // In a more complex implementation, you might maintain a blacklist of tokens
        logger.info("User {} logged out", userPrincipal.getUsername());
        
        return ResponseEntity.ok(MessageResponse.success("Logged out successfully"));
    }

    @GetMapping("/validate-token")
    public ResponseEntity<MessageResponse> validateToken(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        // If we reach here, the JWT filter already validated the token
        return ResponseEntity.ok(MessageResponse.success("Token is valid"));
    }
}