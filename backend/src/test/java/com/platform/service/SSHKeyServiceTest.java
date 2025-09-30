// src/test/java/com/platform/service/SSHKeyServiceTest.java
package com.platform.service;

import com.platform.dto.request.AddSSHKeyRequest;
import com.platform.dto.response.SSHKeyDTO;
import com.platform.exception.BadRequestException;
import com.platform.exception.ResourceNotFoundException;
import com.platform.model.entity.SSHKey;
import com.platform.model.entity.User;
import com.platform.model.enums.UserRole;
import com.platform.repository.SSHKeyRepository;
import com.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SSHKeyServiceTest {

    @Mock
    private SSHKeyRepository sshKeyRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private SSHKeyService sshKeyService;
    
    private User testUser;
    private SSHKey testSSHKey;
    private AddSSHKeyRequest addKeyRequest;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(UserRole.USER);
        
        // Use a more realistic SSH key for testing
        String validSSHKey = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQC7vbqajMzHkE2v5/zGj2Qw8xYeF9ZPm1kJlLm2oPqR3sTuVwXyZ1aBcDeFgHiJkLmNoPqR3sTuVwXyZ1aBcDe test@example.com";
        
        testSSHKey = new SSHKey();
        testSSHKey.setId(1L);
        testSSHKey.setName("Test Key");
        testSSHKey.setPublicKey(validSSHKey);
        testSSHKey.setUser(testUser);
        testSSHKey.setEnabled(true);
        
        addKeyRequest = new AddSSHKeyRequest();
        addKeyRequest.setName("New SSH Key");
        addKeyRequest.setPublicKey(validSSHKey);
    }
    
    @Test
    void addSSHKey_WhenValidKey_ShouldCreateKey() {
        // Arrange
        String username = "testuser";
        
        when(userRepository.findByUsername(username))
            .thenReturn(Optional.of(testUser));
        
        when(sshKeyRepository.countByUser(testUser))
            .thenReturn(2L);
        
        when(sshKeyRepository.existsByUserAndPublicKey(testUser, addKeyRequest.getPublicKey()))
            .thenReturn(false);
        
        when(sshKeyRepository.save(any(SSHKey.class)))
            .thenReturn(testSSHKey);
        
        // Act
        SSHKeyDTO result = sshKeyService.addSSHKey(addKeyRequest, username);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Key");
        assertThat(result.isActive()).isTrue();
        
        verify(userRepository).findByUsername(username);
        verify(sshKeyRepository).countByUser(testUser);
        verify(sshKeyRepository).existsByUserAndPublicKey(testUser, addKeyRequest.getPublicKey());
        verify(sshKeyRepository).save(any(SSHKey.class));
    }
    
    @Test
    void addSSHKey_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        String username = "nonexistent";
        
        when(userRepository.findByUsername(username))
            .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> 
            sshKeyService.addSSHKey(addKeyRequest, username)
        ).isInstanceOf(ResourceNotFoundException.class)
         .hasMessageContaining("User not found");
        
        verify(userRepository).findByUsername(username);
        verifyNoInteractions(sshKeyRepository);
    }
    
    @Test
    void addSSHKey_WhenKeyLimitExceeded_ShouldThrowException() {
        // Arrange
        String username = "testuser";
        
        when(userRepository.findByUsername(username))
            .thenReturn(Optional.of(testUser));
        
        when(sshKeyRepository.countByUser(testUser))
            .thenReturn(5L); // Max is 5
        
        // Act & Assert
        assertThatThrownBy(() -> 
            sshKeyService.addSSHKey(addKeyRequest, username)
        ).isInstanceOf(BadRequestException.class)
         .hasMessageContaining("SSH key limit exceeded");
        
        verify(sshKeyRepository).countByUser(testUser);
        verify(sshKeyRepository, never()).save(any());
    }
    
    @Test
    void addSSHKey_WhenDuplicateKey_ShouldThrowException() {
        // Arrange
        String username = "testuser";
        
        when(userRepository.findByUsername(username))
            .thenReturn(Optional.of(testUser));
        
        when(sshKeyRepository.countByUser(testUser))
            .thenReturn(2L);
        
        when(sshKeyRepository.existsByUserAndPublicKey(testUser, addKeyRequest.getPublicKey()))
            .thenReturn(true);
        
        // Act & Assert
        assertThatThrownBy(() -> 
            sshKeyService.addSSHKey(addKeyRequest, username)
        ).isInstanceOf(BadRequestException.class)
         .hasMessageContaining("SSH key already exists");
        
        verify(sshKeyRepository, never()).save(any());
    }
    
    @Test
    void getUserSSHKeys_WhenUserExists_ShouldReturnKeys() {
        // Arrange
        String username = "testuser";
        List<SSHKey> keys = List.of(testSSHKey);
        
        when(userRepository.findByUsername(username))
            .thenReturn(Optional.of(testUser));
        
        when(sshKeyRepository.findByUserOrderByCreatedAtDesc(testUser))
            .thenReturn(keys);
        
        // Act
        List<SSHKeyDTO> result = sshKeyService.getUserSSHKeys(username);
        
        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Test Key");
        
        verify(userRepository).findByUsername(username);
        verify(sshKeyRepository).findByUserOrderByCreatedAtDesc(testUser);
    }
    
    @Test
    void deleteSSHKey_WhenKeyExistsAndBelongsToUser_ShouldDeleteKey() {
        // Arrange
        Long keyId = 1L;
        String username = "testuser";
        
        when(userRepository.findByUsername(username))
            .thenReturn(Optional.of(testUser));
        
        when(sshKeyRepository.findByIdAndUser(keyId, testUser))
            .thenReturn(Optional.of(testSSHKey));
        
        // Act
        sshKeyService.deleteSSHKey(keyId, username);
        
        // Assert
        verify(sshKeyRepository).delete(testSSHKey);
    }
    
    @Test
    void deleteSSHKey_WhenKeyNotFound_ShouldThrowException() {
        // Arrange
        Long keyId = 999L;
        String username = "testuser";
        
        when(userRepository.findByUsername(username))
            .thenReturn(Optional.of(testUser));
        
        when(sshKeyRepository.findByIdAndUser(keyId, testUser))
            .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> 
            sshKeyService.deleteSSHKey(keyId, username)
        ).isInstanceOf(ResourceNotFoundException.class)
         .hasMessageContaining("SSH key not found");
        
        verify(sshKeyRepository, never()).delete(any());
    }
    
    @Test
    void validatePublicKey_WhenInvalidFormat_ShouldThrowException() {
        // Arrange
        String invalidKey = "invalid-key-format";
        
        // Act & Assert
        assertThatThrownBy(() -> 
            sshKeyService.validatePublicKey(invalidKey)
        ).isInstanceOf(BadRequestException.class)
         .hasMessageContaining("Invalid SSH public key format");
    }
    
    @Test
    void validatePublicKey_WhenValidKey_ShouldNotThrow() {
        // Arrange
        String validKey = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQC7vbqajMzHkE2v5/zGj2Qw8xYeF9ZPm1kJlLm2oPqR3sTuVwXyZ1aBcDeFgHiJkLmNoPqR3sTuVwXyZ1aBcDe test@example.com";
        
        // Act & Assert
        assertThatCode(() -> 
            sshKeyService.validatePublicKey(validKey)
        ).doesNotThrowAnyException();
    }
}