// src/main/java/com/platform/model/entity/Container.java
package com.platform.model.entity;

import com.platform.model.enums.ContainerStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "containers")
@EntityListeners(AuditingEntityListener.class)
public class Container {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String containerId; // Docker container ID

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Column(nullable = false)
    private String image;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContainerStatus status = ContainerStatus.CREATED;

    @Column(name = "ssh_port")
    private Integer sshPort;

    @Column(name = "internal_port")
    private Integer internalPort = 22; // Default SSH port

    @ElementCollection
    @CollectionTable(name = "container_env_vars", 
                    joinColumns = @JoinColumn(name = "container_id"))
    @MapKeyColumn(name = "env_key")
    @Column(name = "env_value")
    private Map<String, String> environmentVariables = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "container_port_mappings", 
                    joinColumns = @JoinColumn(name = "container_id"))
    @MapKeyColumn(name = "host_port")
    @Column(name = "container_port")
    private Map<Integer, Integer> portMappings = new HashMap<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "stopped_at")
    private LocalDateTime stoppedAt;

    // Constructors
    public Container() {}

    public Container(String containerId, String name, String image, User user) {
        this.containerId = containerId;
        this.name = name;
        this.image = image;
        this.user = user;
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

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getStoppedAt() { return stoppedAt; }
    public void setStoppedAt(LocalDateTime stoppedAt) { this.stoppedAt = stoppedAt; }

    // Utility methods
    public void addEnvironmentVariable(String key, String value) {
        this.environmentVariables.put(key, value);
    }

    public void addPortMapping(Integer hostPort, Integer containerPort) {
        this.portMappings.put(hostPort, containerPort);
    }

    public boolean isRunning() {
        return status == ContainerStatus.RUNNING;
    }

    @Override
    public String toString() {
        return "Container{" +
                "id=" + id +
                ", containerId='" + containerId + '\'' +
                ", name='" + name + '\'' +
                ", image='" + image + '\'' +
                ", status=" + status +
                ", sshPort=" + sshPort +
                '}';
    }
}