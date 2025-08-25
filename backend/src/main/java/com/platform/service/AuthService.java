// src/main/java/com/platform/service/AuthService.java
package com.platform.service;

import com.platform.dto.request.LoginRequest;
import com.platform.dto.request.RegisterRequest;
import com.platform.dto.response.AuthResponse;
import com.platform.dto.response.UserDTO;
import com.platform.exception.BadRequestException;
import com.platform.exception.UserAlreadyExistsException;
import com.platform.model.entity.User;
import com.platform.model.enums.UserRole;
import com.platform.repository.UserRepository;
import com.platform.security.jwt.JwtUtil;
import com.platform.security.jwt.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    public AuthResponse login(LoginRequest loginRequest) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );

            // Generate tokens
            String accessToken = jwtUtil.generateToken(authentication);
            String refreshToken = jwtUtil.generateRefreshToken(authentication);

            // Get user info
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

            logger.info("User {} logged in successfully", userPrincipal.getUsername());

            return new AuthResponse(
                accessToken,
                refreshToken,
                jwtExpirationMs / 1000, // Convert to seconds
                UserDTO.fromUser(user).toUserInfo()
            );

        } catch (AuthenticationException e) {
            logger.warn("Failed login attempt for username: {}", loginRequest.getUsername());
            throw new BadRequestException("Invalid username or password");
        }
    }

    public AuthResponse register(RegisterRequest registerRequest) {
        // Check if username already exists
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new UserAlreadyExistsException("Username is already taken");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new UserAlreadyExistsException("Email is already in use");
        }

        // Create new user
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setRole(UserRole.USER); // Default role
        user.setEnabled(true);

        user = userRepository.save(user);

        logger.info("New user registered: {} ({})", user.getUsername(), user.getEmail());

        // Auto-login after registration
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                registerRequest.getUsername(),
                registerRequest.getPassword()
            )
        );

        String accessToken = jwtUtil.generateToken(authentication);
        String refreshToken = jwtUtil.generateRefreshToken(authentication);

        return new AuthResponse(
            accessToken,
            refreshToken,
            jwtExpirationMs / 1000,
            UserDTO.fromUser(user).toUserInfo()
        );
    }

    public AuthResponse refreshToken(String refreshToken) {
        // Validate refresh token
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BadRequestException("Invalid refresh token");
        }

        // Check if it's actually a refresh token
        if (!jwtUtil.isRefreshToken(refreshToken)) {
            throw new BadRequestException("Token is not a refresh token");
        }

        // Get user from refresh token
        String username = jwtUtil.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new BadRequestException("User not found"));

        // Check if user is still enabled
        if (!user.getEnabled()) {
            throw new BadRequestException("User account is disabled");
        }

        // Create authentication object for token generation
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            userPrincipal, null, userPrincipal.getAuthorities()
        );

        // Generate new tokens
        String newAccessToken = jwtUtil.generateToken(authentication);
        String newRefreshToken = jwtUtil.generateRefreshToken(authentication);

        logger.debug("Tokens refreshed for user: {}", username);

        return new AuthResponse(
            newAccessToken,
            newRefreshToken,
            jwtExpirationMs / 1000,
            UserDTO.fromUser(user).toUserInfo()
        );
    }

    @Transactional(readOnly = true)
    public UserDTO getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        return UserDTO.fromUser(user);
    }

    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }
}