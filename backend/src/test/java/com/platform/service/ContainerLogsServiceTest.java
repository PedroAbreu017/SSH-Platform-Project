// src/test/java/com/platform/service/ContainerLogsServiceTest.java
package com.platform.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.platform.model.entity.Container;
import com.platform.model.entity.User;
import com.platform.model.enums.ContainerStatus;
import com.platform.model.enums.UserRole;
import com.platform.repository.ContainerRepository;
import com.platform.repository.UserRepository;
import com.platform.util.LogContainerResultCallback;
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
class ContainerLogsServiceTest {

    @Mock
    private DockerClient dockerClient;
    
    @Mock
    private ContainerRepository containerRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private LogContainerCmd logContainerCmd;
    
    @InjectMocks
    private ContainerService containerService;
    
    private User testUser;
    private Container testContainer;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(UserRole.USER);
        
        testContainer = new Container();
        testContainer.setId(1L);
        testContainer.setContainerId("container123");
        testContainer.setName("test-container");
        testContainer.setImage("ubuntu:20.04");
        testContainer.setStatus(ContainerStatus.RUNNING);
        testContainer.setUser(testUser);
        testContainer.setSshPort(8001);
    }
    
    @Test
    void getContainerLogs_WhenContainerExists_ShouldReturnLogs() throws Exception {
        // Arrange
        String username = "testuser";
        int lines = 100;
        
        when(userRepository.findByUsername(username))
            .thenReturn(Optional.of(testUser));
        
        when(containerRepository.findByIdAndUser(1L, testUser))
            .thenReturn(Optional.of(testContainer));
        
        when(dockerClient.logContainerCmd("container123"))
            .thenReturn(logContainerCmd);
        
        when(logContainerCmd.withStdOut(true))
            .thenReturn(logContainerCmd);
        
        when(logContainerCmd.withStdErr(true))
            .thenReturn(logContainerCmd);
        
        when(logContainerCmd.withTail(lines))
            .thenReturn(logContainerCmd);
        
        // Mock the exec method to simulate callback execution
        when(logContainerCmd.exec(any(LogContainerResultCallback.class)))
            .thenAnswer(invocation -> {
                LogContainerResultCallback callback = invocation.getArgument(0);
                // Simulate adding logs to the callback
                callback.onNext(createLogFrame("Log line 1"));
                callback.onNext(createLogFrame("Log line 2"));
                callback.onNext(createLogFrame("Log line 3"));
                callback.onComplete();
                return callback;
            });
        
        // Act
        List<String> logs = containerService.getContainerLogs(1L, username, lines);
        
        // Assert
        assertThat(logs).isNotNull();
        assertThat(logs).hasSize(3);
        assertThat(logs).contains("Log line 1", "Log line 2", "Log line 3");
        
        // Verify interactions
        verify(userRepository).findByUsername(username);
        verify(containerRepository).findByIdAndUser(1L, testUser);
        verify(dockerClient).logContainerCmd("container123");
        verify(logContainerCmd).withStdOut(true);
        verify(logContainerCmd).withStdErr(true);
        verify(logContainerCmd).withTail(lines);
    }
    
    // Helper method to create log frames
    private com.github.dockerjava.api.model.Frame createLogFrame(String content) {
        return new com.github.dockerjava.api.model.Frame(
            com.github.dockerjava.api.model.StreamType.STDOUT,
            content.getBytes()
        );
    }
    
    @Test
    void getContainerLogs_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        String username = "nonexistent";
        
        when(userRepository.findByUsername(username))
            .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> 
            containerService.getContainerLogs(1L, username, 100)
        ).hasMessageContaining("User not found");
        
        verify(userRepository).findByUsername(username);
        verifyNoInteractions(containerRepository, dockerClient);
    }
    
    @Test
    void getContainerLogs_WhenContainerNotFound_ShouldThrowException() {
        // Arrange
        String username = "testuser";
        
        when(userRepository.findByUsername(username))
            .thenReturn(Optional.of(testUser));
        
        when(containerRepository.findByIdAndUser(1L, testUser))
            .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> 
            containerService.getContainerLogs(1L, username, 100)
        ).hasMessageContaining("Container not found");
        
        verify(userRepository).findByUsername(username);
        verify(containerRepository).findByIdAndUser(1L, testUser);
        verifyNoInteractions(dockerClient);
    }
    
    @Test
    void getContainerLogs_WhenDockerThrowsException_ShouldThrowBadRequest() {
        // Arrange
        String username = "testuser";
        
        when(userRepository.findByUsername(username))
            .thenReturn(Optional.of(testUser));
        
        when(containerRepository.findByIdAndUser(1L, testUser))
            .thenReturn(Optional.of(testContainer));
        
        when(dockerClient.logContainerCmd("container123"))
            .thenThrow(new RuntimeException("Docker error"));
        
        // Act & Assert
        assertThatThrownBy(() -> 
            containerService.getContainerLogs(1L, username, 100)
        ).hasMessageContaining("Failed to get container logs");
        
        verify(dockerClient).logContainerCmd("container123");
    }
}