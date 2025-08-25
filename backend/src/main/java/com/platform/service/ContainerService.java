// src/main/java/com/platform/service/ContainerService.java
package com.platform.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import com.platform.dto.request.CreateContainerRequest;
import com.platform.dto.response.ContainerDTO;
import com.platform.exception.BadRequestException;
import com.platform.exception.ResourceNotFoundException;
import com.platform.model.entity.Container;
import com.platform.model.entity.User;
import com.platform.model.enums.ContainerStatus;
import com.platform.repository.ContainerRepository;
import com.platform.repository.UserRepository;
import com.platform.util.LogContainerResultCallback;
import com.platform.util.PullImageResultCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class ContainerService {

    private static final Logger logger = LoggerFactory.getLogger(ContainerService.class);

    @Autowired
    private DockerClient dockerClient;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private UserRepository userRepository;

    // Allowed images for security
    private static final List<String> ALLOWED_IMAGES = List.of(
        "ubuntu:22.04", "ubuntu:20.04", "ubuntu:18.04",
        "debian:11", "debian:10",
        "alpine:3.18", "alpine:3.17",
        "node:18", "node:16", "node:20",
        "python:3.11", "python:3.10", "python:3.9",
        "openjdk:17", "openjdk:11",
        "nginx:alpine", "nginx:latest",
        "rastasheep/ubuntu-sshd:18.04",
        "danielguerra/ubuntu-xrdp:18.04"
    );

    public ContainerDTO createContainer(CreateContainerRequest request, String username) {
    // Validate user
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    // Validate image
    if (!ALLOWED_IMAGES.contains(request.getImage())) {
        throw new BadRequestException("Image not allowed: " + request.getImage());
    }

    // Check container limit per user (5 containers max)
    long userContainerCount = containerRepository.countByUserAndStatusIn(
        user, List.of(ContainerStatus.RUNNING, ContainerStatus.CREATED, ContainerStatus.PAUSED)
    );
    if (userContainerCount >= 5) {
        throw new BadRequestException("Container limit reached (max 5 containers per user)");
    }

    try {
        // Pull image if not exists
        pullImageIfNeeded(request.getImage());

        // Find available SSH port
        int sshPort = findAvailablePort();

        // Prepare environment variables
        List<String> envVars = new ArrayList<>();
        envVars.add("CONTAINER_USER=" + username);
        envVars.add("CONTAINER_ID=" + request.getName());
        if (request.getEnvironmentVariables() != null) {
            request.getEnvironmentVariables().forEach((key, value) -> 
                envVars.add(key + "=" + value)
            );
        }

        // Get container configuration based on image type
        ContainerConfig config = getContainerConfig(request.getImage());

        // Create container with appropriate configuration
        CreateContainerResponse dockerContainer = dockerClient.createContainerCmd(request.getImage())
            .withName(generateContainerName(username, request.getName()))
            .withEnv(envVars)
            .withCmd(config.getCommand())
            .withExposedPorts(ExposedPort.tcp(22)) // SSH port
            .withHostConfig(
                HostConfig.newHostConfig()
                    .withPortBindings(new PortBinding(Ports.Binding.bindPort(sshPort), ExposedPort.tcp(22)))
                    .withMemory(512L * 1024 * 1024) // 512MB limit
                    .withCpuQuota(50000L) // 50% CPU limit
                    .withRestartPolicy(RestartPolicy.noRestart())
            )
            .withWorkingDir("/workspace")
            .withTty(true)
            .exec();

        // Save container info to database
        Container container = new Container();
        container.setContainerId(dockerContainer.getId());
        container.setName(request.getName());
        container.setImage(request.getImage());
        container.setUser(user);
        container.setSshPort(sshPort);
        container.setStatus(ContainerStatus.CREATED);
        
        if (request.getEnvironmentVariables() != null) {
            container.setEnvironmentVariables(request.getEnvironmentVariables());
        }
        
        Map<Integer, Integer> portMappings = new HashMap<>();
        portMappings.put(sshPort, 22);
        container.setPortMappings(portMappings);

        container = containerRepository.save(container);

        logger.info("Container created: {} for user: {}", dockerContainer.getId(), username);

        return ContainerDTO.fromContainer(container);

    } catch (Exception e) {
        logger.error("Failed to create container for user: " + username, e);
        throw new BadRequestException("Failed to create container: " + e.getMessage());
    }
}

// Novo método para configurações específicas por imagem
private ContainerConfig getContainerConfig(String image) {
    if (image.startsWith("alpine")) {
        return new ContainerConfig(new String[]{
            "/bin/sh", "-c", 
            "apk add --no-cache openssh-server && " +
            "ssh-keygen -A && " +
            "echo 'root:password' | chpasswd && " +
            "sed -i 's/#PermitRootLogin prohibit-password/PermitRootLogin yes/' /etc/ssh/sshd_config && " +
            "sed -i 's/#PasswordAuthentication yes/PasswordAuthentication yes/' /etc/ssh/sshd_config && " +
            "mkdir -p /var/run/sshd && " +
            "/usr/sbin/sshd -D"
        });
    } 
    else if (image.contains("ubuntu-sshd") || image.contains("sshd")) {
        // Para imagens que já têm SSH configurado
        return new ContainerConfig(new String[]{
            "/bin/bash", "-c",
            "service ssh start && tail -f /dev/null"
        });
    }
    else if (image.startsWith("ubuntu") || image.startsWith("debian")) {
        return new ContainerConfig(new String[]{
            "/bin/bash", "-c", 
            "export DEBIAN_FRONTEND=noninteractive && " +
            "apt-get update -qq && " +
            "apt-get install -y -qq openssh-server && " +
            "mkdir -p /var/run/sshd && " +
            "echo 'root:password' | chpasswd && " +
            "sed -i 's/#PermitRootLogin prohibit-password/PermitRootLogin yes/' /etc/ssh/sshd_config && " +
            "sed -i 's/#PasswordAuthentication yes/PasswordAuthentication yes/' /etc/ssh/sshd_config && " +
            "/usr/sbin/sshd -D"
        });
    }
    else {
        // Default fallback - manter container vivo
        return new ContainerConfig(new String[]{
            "/bin/sh", "-c", "tail -f /dev/null"
        });
    }
}

private static class ContainerConfig {
    private final String[] command;

    public ContainerConfig(String[] command) {
        this.command = command;
    }

    public String[] getCommand() {
        return command;
    }
}


    public ContainerDTO startContainer(Long containerId, String username) {
        Container container = findContainerByIdAndUser(containerId, username);
        
        try {
            dockerClient.startContainerCmd(container.getContainerId()).exec();
            
            container.setStatus(ContainerStatus.RUNNING);
            container.setStartedAt(java.time.LocalDateTime.now());
            container = containerRepository.save(container);
            
            logger.info("Container started: {} by user: {}", container.getContainerId(), username);
            
            return ContainerDTO.fromContainer(container);
            
        } catch (Exception e) {
            logger.error("Failed to start container: " + container.getContainerId(), e);
            throw new BadRequestException("Failed to start container: " + e.getMessage());
        }
    }

    public ContainerDTO stopContainer(Long containerId, String username) {
        Container container = findContainerByIdAndUser(containerId, username);
        
        try {
            dockerClient.stopContainerCmd(container.getContainerId()).exec();
            
            container.setStatus(ContainerStatus.EXITED);
            container.setStoppedAt(java.time.LocalDateTime.now());
            container = containerRepository.save(container);
            
            logger.info("Container stopped: {} by user: {}", container.getContainerId(), username);
            
            return ContainerDTO.fromContainer(container);
            
        } catch (Exception e) {
            logger.error("Failed to stop container: " + container.getContainerId(), e);
            throw new BadRequestException("Failed to stop container: " + e.getMessage());
        }
    }

    public void deleteContainer(Long containerId, String username) {
        Container container = findContainerByIdAndUser(containerId, username);
        
        try {
            // Stop container if running
            if (container.isRunning()) {
                dockerClient.stopContainerCmd(container.getContainerId()).exec();
            }
            
            // Remove container from Docker
            dockerClient.removeContainerCmd(container.getContainerId())
                .withForce(true)
                .exec();
            
            // Remove from database
            containerRepository.delete(container);
            
            logger.info("Container deleted: {} by user: {}", container.getContainerId(), username);
            
        } catch (Exception e) {
            logger.error("Failed to delete container: " + container.getContainerId(), e);
            throw new BadRequestException("Failed to delete container: " + e.getMessage());
        }
    }

    public List<ContainerDTO> getUserContainers(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        List<Container> containers = containerRepository.findByUser(user);
        
        // Update container statuses from Docker
        containers.forEach(this::updateContainerStatus);
        
        return containers.stream()
            .map(ContainerDTO::fromContainer)
            .collect(Collectors.toList());
    }

    public ContainerDTO getContainer(Long containerId, String username) {
        Container container = findContainerByIdAndUser(containerId, username);
        updateContainerStatus(container);
        return ContainerDTO.fromContainer(container);
    }

    public List<String> getContainerLogs(Long containerId, String username, int lines) {
        Container container = findContainerByIdAndUser(containerId, username);
        
        try {
            LogContainerResultCallback callback = new LogContainerResultCallback();
            dockerClient.logContainerCmd(container.getContainerId())
                .withStdOut(true)
                .withStdErr(true)
                .withTail(lines)
                .exec(callback)
                .awaitCompletion();
                
            return callback.getLogs();
                
        } catch (Exception e) {
            logger.error("Failed to get logs for container: " + container.getContainerId(), e);
            throw new BadRequestException("Failed to get container logs: " + e.getMessage());
        }
    }

    // Helper methods
    private Container findContainerByIdAndUser(Long containerId, String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        return containerRepository.findByIdAndUser(containerId, user)
            .orElseThrow(() -> new ResourceNotFoundException("Container not found"));
    }

    private void updateContainerStatus(Container container) {
        try {
            InspectContainerResponse dockerContainer = dockerClient
                .inspectContainerCmd(container.getContainerId())
                .exec();
            
            ContainerStatus newStatus = ContainerStatus.fromDockerStatus(
                dockerContainer.getState().getStatus()
            );
            
            if (!newStatus.equals(container.getStatus())) {
                container.setStatus(newStatus);
                containerRepository.save(container);
            }
            
        } catch (NotFoundException e) {
            // Container was removed from Docker but still in database
            container.setStatus(ContainerStatus.DEAD);
            containerRepository.save(container);
        } catch (Exception e) {
            logger.warn("Failed to update container status: " + container.getContainerId(), e);
        }
    }

    private void pullImageIfNeeded(String image) {
        try {
            dockerClient.inspectImageCmd(image).exec();
        } catch (NotFoundException e) {
            logger.info("Pulling image: {}", image);
            try {
                dockerClient.pullImageCmd(image)
                    .exec(new PullImageResultCallback())
                    .awaitCompletion();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new BadRequestException("Image pull interrupted: " + image);
            }
        }
    }

    private int findAvailablePort() {
        // Find available port between 8000-9000
        List<Integer> usedPorts = containerRepository.findAllSshPorts();
        
        for (int port = 8000; port <= 9000; port++) {
            if (!usedPorts.contains(port)) {
                return port;
            }
        }
        
        throw new BadRequestException("No available ports for SSH");
    }

    private String generateContainerName(String username, String name) {
        return String.format("platform_%s_%s_%d", username, name, System.currentTimeMillis());
    }
}