// src/test/java/com/platform/controller/SSHKeyControllerTest.java
package com.platform.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.dto.request.AddSSHKeyRequest;
import com.platform.dto.request.LoginRequest;
import com.platform.integration.BaseIntegrationTest;
import com.platform.model.entity.SSHKey;
import com.platform.model.entity.User;
import com.platform.model.enums.UserRole;
import com.platform.repository.SSHKeyRepository;
import com.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class SSHKeyControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SSHKeyRepository sshKeyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private String jwtToken;
    private String testPassword = "password123";
    private String validSSHKey = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQC7vbqajMzHkE2v5/zGj2Qw8xYeF9ZPm1kJlLm2oPqR3sTuVwXyZ1aBcDeFgHiJkLmNoPqR3sTuVwXyZ1aBcDeFgHiJkLmNoPqR3sTuVwXyZ1aBcDe test@example.com";

    @BeforeEach
    void setUp() throws Exception {
        // Clean up
        sshKeyRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user with encoded password
        testUser = new User();
        testUser.setUsername("testuser" + System.currentTimeMillis());
        testUser.setEmail(testUser.getUsername() + "@example.com");
        testUser.setPassword(passwordEncoder.encode(testPassword)); // Password encoded
        testUser.setRole(UserRole.USER);
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);

        // Get JWT token via login API
        jwtToken = performLoginAndGetToken(testUser.getUsername(), testPassword);
    }

    /**
     * Perform login via API and extract JWT token from response
     */
    private String performLoginAndGetToken(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseBody);
        
        return responseJson.get("accessToken").asText();
    }

    // ==========================
    // Testes de criação de chave
    // ==========================
    @Test
    @Transactional
    void addSSHKey_WhenValidRequest_ShouldReturnCreated() throws Exception {
        // Arrange
        AddSSHKeyRequest request = new AddSSHKeyRequest();
        request.setName("Test SSH Key");
        request.setPublicKey(validSSHKey);

        // Act & Assert
        mockMvc.perform(post("/api/ssh-keys")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test SSH Key"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.publicKeyType").value("rsa"));

        // Verify database
        assertThat(sshKeyRepository.count()).isEqualTo(1);
        SSHKey savedKey = sshKeyRepository.findAll().get(0);
        assertThat(savedKey.getName()).isEqualTo("Test SSH Key");
        assertThat(savedKey.getEnabled()).isTrue();
        assertThat(savedKey.getUser().getUsername()).isEqualTo(testUser.getUsername());
    }

    @Test
    @Transactional
    void addSSHKey_WhenInvalidKey_ShouldReturnBadRequest() throws Exception {
        // Arrange
        AddSSHKeyRequest request = new AddSSHKeyRequest();
        request.setName("Invalid Key");
        request.setPublicKey("invalid-ssh-key");

        // Act & Assert
        mockMvc.perform(post("/api/ssh-keys")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        // Verify no key was created
        assertThat(sshKeyRepository.count()).isEqualTo(0);
    }

    @Test
    @Transactional
    void addSSHKey_WhenDuplicateKey_ShouldReturnBadRequest() throws Exception {
        // Arrange - create existing key
        SSHKey existingKey = createSSHKeyForUser("Existing Key", validSSHKey, testUser);
        sshKeyRepository.save(existingKey);

        AddSSHKeyRequest request = new AddSSHKeyRequest();
        request.setName("Duplicate Key");
        request.setPublicKey(validSSHKey);

        // Act & Assert
        mockMvc.perform(post("/api/ssh-keys")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        // Verify only one key exists
        assertThat(sshKeyRepository.count()).isEqualTo(1);
    }

    @Test
    void addSSHKey_WhenNotAuthenticated_ShouldReturnForbidden() throws Exception {
        // Arrange
        AddSSHKeyRequest request = new AddSSHKeyRequest();
        request.setName("Test Key");
        request.setPublicKey(validSSHKey);

        // Act & Assert
        mockMvc.perform(post("/api/ssh-keys")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void addSSHKey_WhenBlankName_ShouldReturnBadRequest() throws Exception {
        // Arrange
        AddSSHKeyRequest request = new AddSSHKeyRequest();
        request.setName(""); // Nome em branco - vai falhar na validação @NotBlank
        request.setPublicKey(validSSHKey);

        // Act & Assert
        mockMvc.perform(post("/api/ssh-keys")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==========================
    // Testes de listagem de chaves
    // ==========================
    @Test
    @Transactional
    void getUserSSHKeys_WhenKeysExist_ShouldReturnList() throws Exception {
        // Arrange
        SSHKey key1 = createSSHKeyForUser("Key 1", validSSHKey, testUser);
        SSHKey key2 = createSSHKeyForUser("Key 2", generateDifferentSSHKey(), testUser);
        sshKeyRepository.save(key1);
        sshKeyRepository.save(key2);

        // Act & Assert
        mockMvc.perform(get("/api/ssh-keys")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[1].name").exists());
    }

    @Test
    @Transactional
    void getUserSSHKeys_WhenNoKeys_ShouldReturnEmptyList() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/ssh-keys")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ==========================
    // Testes de exclusão de chaves
    // ==========================
    @Test
    @Transactional
    void deleteSSHKey_WhenKeyExists_ShouldReturnSuccess() throws Exception {
        // Arrange
        SSHKey sshKey = createSSHKeyForUser("Test Key", validSSHKey, testUser);
        sshKey = sshKeyRepository.save(sshKey);

        // Act & Assert
        mockMvc.perform(delete("/api/ssh-keys/{id}", sshKey.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("SSH key deleted successfully"));

        // Verify key was deleted
        assertThat(sshKeyRepository.findById(sshKey.getId())).isEmpty();
    }

    @Test
    @Transactional
    void deleteSSHKey_WhenKeyNotFound_ShouldReturnNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/ssh-keys/999")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    // ==========================
    // Teste de estatísticas
    // ==========================
    @Test
    @Transactional
    void getSSHKeyStats_WhenKeysExist_ShouldReturnStats() throws Exception {
        // Arrange
        SSHKey activeKey1 = createSSHKeyForUser("Active Key 1", validSSHKey, testUser);
        SSHKey activeKey2 = createSSHKeyForUser("Active Key 2", generateDifferentSSHKey(), testUser);
        SSHKey inactiveKey = createSSHKeyForUser("Inactive Key", generateThirdSSHKey(), testUser);

        activeKey1.setEnabled(true);
        activeKey2.setEnabled(true);
        inactiveKey.setEnabled(false);

        sshKeyRepository.save(activeKey1);
        sshKeyRepository.save(activeKey2);
        sshKeyRepository.save(inactiveKey);

        // Act & Assert
        mockMvc.perform(get("/api/ssh-keys/stats")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalKeys").value(3))
                .andExpect(jsonPath("$.activeKeys").value(2))
                .andExpect(jsonPath("$.inactiveKeys").value(1));
    }

    @Test
    @Transactional
    void getSSHKeyStats_WhenNoKeys_ShouldReturnZeroStats() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/ssh-keys/stats")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalKeys").value(0))
                .andExpect(jsonPath("$.activeKeys").value(0))
                .andExpect(jsonPath("$.inactiveKeys").value(0));
    }

    // ==========================
    // Helpers
    // ==========================
    private SSHKey createSSHKeyForUser(String name, String publicKey, User user) {
        SSHKey sshKey = new SSHKey();
        sshKey.setName(name);
        sshKey.setPublicKey(publicKey);
        sshKey.setUser(user);
        sshKey.setEnabled(true);
        return sshKey;
    }

    private String generateDifferentSSHKey() {
        return "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQDifferentKeyDataHere12345abcdefghijklmnopqrstuvwxyz1234567890ABCDEF test2@example.com";
    }

    private String generateThirdSSHKey() {
        return "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIThirdKeyDataHere67890ghijklmnopqrstuvwxyzABCDEF1234567890 test3@example.com";
    }
}    