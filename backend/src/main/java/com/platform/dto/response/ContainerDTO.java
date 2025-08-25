package com.platform.dto.response;

import com.platform.model.entity.Container;
import com.platform.model.enums.ContainerStatus;

import java.time.LocalDateTime;
import java.util.Map;

public class ContainerDTO {
    private Long id;
    private String containerId;
    private String name;
    private String image;
    private ContainerStatus status;
    private Integer sshPort;
    private Integer internalPort;
    private Map<String, String> environmentVariables;
    private Map<Integer, Integer> portMappings;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime startedAt;
    private LocalDateTime stoppedAt;
    private String sshConnectionString;

    public ContainerDTO() {}

    public static ContainerDTO fromContainer(Container container) {
        ContainerDTO dto = new ContainerDTO();
        dto.setId(container.getId());
        dto.setContainerId(container.getContainerId());
        dto.setName(container.getName());
        dto.setImage(container.getImage());
        dto.setStatus(container.getStatus());
        dto.setSshPort(container.getSshPort());
        dto.setInternalPort(container.getInternalPort());
        dto.setEnvironmentVariables(container.getEnvironmentVariables());
        dto.setPortMappings(container.getPortMappings());
        dto.setCreatedAt(container.getCreatedAt());
        dto.setUpdatedAt(container.getUpdatedAt());
        dto.setStartedAt(container.getStartedAt());
        dto.setStoppedAt(container.getStoppedAt());
        
        // Generate SSH connection string
        if (container.getSshPort() != null && container.getStatus().canConnect()) {
            dto.setSshConnectionString(
                String.format("ssh -p %d %s@localhost", 
                    container.getSshPort(), 
                    container.getUser().getUsername())
            );
        }
        
        return dto;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContainerId() { return containerId; }
    public void setContainerId(String containerId) { this.containerId = containerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public ContainerStatus getStatus() { return status; }
    public void setStatus(ContainerStatus status) { this.status = status; }

    public Integer getSshPort() { return sshPort; }
    public void setSshPort(Integer sshPort) { this.sshPort = sshPort; }

    public Integer getInternalPort() { return internalPort; }
    public void setInternalPort(Integer internalPort) { this.internalPort = internalPort; }

    public Map<String, String> getEnvironmentVariables() { return environmentVariables; }
    public void setEnvironmentVariables(Map<String, String> environmentVariables) { 
        this.environmentVariables = environmentVariables; 
    }

    public Map<Integer, Integer> getPortMappings() { return portMappings; }
    public void setPortMappings(Map<Integer, Integer> portMappings) { 
        this.portMappings = portMappings; 
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getStoppedAt() { return stoppedAt; }
    public void setStoppedAt(LocalDateTime stoppedAt) { this.stoppedAt = stoppedAt; }

    public String getSshConnectionString() { return sshConnectionString; }
    public void setSshConnectionString(String sshConnectionString) { 
        this.sshConnectionString = sshConnectionString; 
    }

    // Utility methods
    public boolean isRunning() {
        return status == ContainerStatus.RUNNING;
    }

    public boolean canConnect() {
        return status != null && status.canConnect();
    }

    public String getStatusDisplay() {
        return status != null ? status.name().toLowerCase() : "unknown";
    }
}
