package com.platform.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.platform.exception.ResourceNotFoundException;
import com.platform.model.entity.Container;
import com.platform.model.entity.User;
import com.platform.model.enums.ContainerStatus;
import com.platform.model.enums.UserRole;
import com.platform.repository.ContainerRepository;
import com.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void getContainerLogs_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        String username = "nonexistent";
        
        when(userRepository.findByUsername(username))
            .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> 
            containerService.getContainerLogs(1L, username, 100)
        ).isInstanceOf(ResourceNotFoundException.class)
         .hasMessageContaining("User not found");
        
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
        ).isInstanceOf(ResourceNotFoundException.class)
         .hasMessageContaining("Container not found");
        
        verify(userRepository).findByUsername(username);
        verify(containerRepository).findByIdAndUser(1L, testUser);
        verifyNoInteractions(dockerClient);
    }
    
    @Test
    void getContainerLogs_WhenDockerClientCalled_ShouldConfigureCommand() {
        // Arrange
        String username = "testuser";
        
        when(userRepository.findByUsername(username))
            .thenReturn(Optional.of(testUser));
        
        when(containerRepository.findByIdAndUser(1L, testUser))
            .thenReturn(Optional.of(testContainer));
        
        when(dockerClient.logContainerCmd("container123"))
            .thenReturn(logContainerCmd);
        
        when(logContainerCmd.withStdOut(anyBoolean()))
            .thenReturn(logContainerCmd);
        
        when(logContainerCmd.withStdErr(anyBoolean()))
            .thenReturn(logContainerCmd);
        
        when(logContainerCmd.withTail(anyInt()))
            .thenReturn(logContainerCmd);
        
        // Act - vai falhar ao executar mas testamos a configuração
        try {
            containerService.getContainerLogs(1L, username, 100);
        } catch (Exception e) {
            // Esperado - o exec() vai falhar mas já verificamos o setup
        }
        
        // Assert
        verify(dockerClient).logContainerCmd("container123");
        verify(logContainerCmd).withStdOut(true);
        verify(logContainerCmd).withStdErr(true);
        verify(logContainerCmd).withTail(100);
    }
}