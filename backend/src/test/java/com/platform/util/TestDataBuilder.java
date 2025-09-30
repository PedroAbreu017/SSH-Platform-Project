// src/test/java/com/platform/util/TestDataBuilder.java
package com.platform.util;

import com.platform.model.entity.Container;
import com.platform.model.entity.User;
import com.platform.model.enums.UserRole;
import com.platform.model.enums.ContainerStatus;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class TestDataBuilder {
    
    public static User createTestUser() {
        User user = new User();
        user.setUsername("testuser" + System.currentTimeMillis());
        user.setEmail("test" + System.currentTimeMillis() + "@example.com");
        user.setPassword("password123");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(UserRole.USER);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
    
    public static User createAdminUser() {
        User user = createTestUser();
        user.setUsername("admin" + System.currentTimeMillis());
        user.setEmail("admin" + System.currentTimeMillis() + "@example.com");
        user.setRole(UserRole.ADMIN);
        return user;
    }
    
    public static Container createTestContainer(User user) {
        Container container = new Container();
        container.setName("test-container-" + System.currentTimeMillis());
        container.setContainerId("mock-container-id-" + System.currentTimeMillis());
        container.setImage("alpine:3.18");
        container.setStatus(ContainerStatus.CREATED);
        container.setSshPort(8001);
        container.setInternalPort(22);
        container.setUser(user);
        container.setEnvironmentVariables(createTestEnvironmentVariables());
        container.setPortMappings(createTestPortMappings());
        container.setCreatedAt(LocalDateTime.now());
        container.setUpdatedAt(LocalDateTime.now());
        return container;
    }
    
    public static Container createRunningContainer(User user) {
        Container container = createTestContainer(user);
        container.setStatus(ContainerStatus.RUNNING);
        container.setStartedAt(LocalDateTime.now());
        return container;
    }
    
    public static Map<String, String> createTestEnvironmentVariables() {
        Map<String, String> envVars = new HashMap<>();
        envVars.put("TEST_VAR", "test_value");
        envVars.put("ENVIRONMENT", "test");
        return envVars;
    }
    
    public static Map<Integer, Integer> createTestPortMappings() {
        Map<Integer, Integer> portMappings = new HashMap<>();
        portMappings.put(8001, 22);
        return portMappings;
    }
    
    public static String createValidRSAPublicKey() {
        return "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC5Y5j8Kp7Uh3xE9L5gUNs6fWQ7Qz3rT8yVp2nMo1sF4dGh6jK test@example.com";
    }
    
    public static String createValidED25519PublicKey() {
        return "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIOGP2Y7Y1N1vL8X2K9M3F4d5G6h7I8j9K0l1M2n3O4p5Q6r7S8t9U0v1W2x3Y4z5A6b7C8d9E0f1 test@example.com";
    }
}