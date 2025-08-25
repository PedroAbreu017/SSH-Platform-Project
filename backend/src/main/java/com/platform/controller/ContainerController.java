// src/main/java/com/platform/controller/ContainerController.java
package com.platform.controller;

import com.platform.dto.request.CreateContainerRequest;
import com.platform.dto.response.ContainerDTO;
import com.platform.dto.response.MessageResponse;
import com.platform.security.jwt.UserPrincipal;
import com.platform.service.ContainerService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/containers")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ContainerController {

    private static final Logger logger = LoggerFactory.getLogger(ContainerController.class);

    @Autowired
    private ContainerService containerService;

    @PostMapping
    public ResponseEntity<ContainerDTO> createContainer(
            @Valid @RequestBody CreateContainerRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.info("Creating container '{}' with image '{}' for user: {}", 
                   request.getName(), request.getImage(), userPrincipal.getUsername());
        
        ContainerDTO container = containerService.createContainer(request, userPrincipal.getUsername());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(container);
    }

    @GetMapping
    public ResponseEntity<List<ContainerDTO>> getUserContainers(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.debug("Fetching containers for user: {}", userPrincipal.getUsername());
        
        List<ContainerDTO> containers = containerService.getUserContainers(userPrincipal.getUsername());
        
        return ResponseEntity.ok(containers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContainerDTO> getContainer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.debug("Fetching container {} for user: {}", id, userPrincipal.getUsername());
        
        ContainerDTO container = containerService.getContainer(id, userPrincipal.getUsername());
        
        return ResponseEntity.ok(container);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<ContainerDTO> startContainer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.info("Starting container {} for user: {}", id, userPrincipal.getUsername());
        
        ContainerDTO container = containerService.startContainer(id, userPrincipal.getUsername());
        
        return ResponseEntity.ok(container);
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<ContainerDTO> stopContainer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.info("Stopping container {} for user: {}", id, userPrincipal.getUsername());
        
        ContainerDTO container = containerService.stopContainer(id, userPrincipal.getUsername());
        
        return ResponseEntity.ok(container);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteContainer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.info("Deleting container {} for user: {}", id, userPrincipal.getUsername());
        
        containerService.deleteContainer(id, userPrincipal.getUsername());
        
        return ResponseEntity.ok(MessageResponse.success("Container deleted successfully"));
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<List<String>> getContainerLogs(
            @PathVariable Long id,
            @RequestParam(defaultValue = "100") int lines,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        logger.debug("Fetching logs for container {} (lines: {}) for user: {}", 
                    id, lines, userPrincipal.getUsername());
        
        List<String> logs = containerService.getContainerLogs(id, userPrincipal.getUsername(), lines);
        
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/images")
    public ResponseEntity<List<String>> getAvailableImages() {
        List<String> images = List.of(
            "ubuntu:22.04", "ubuntu:20.04", "ubuntu:18.04",
            "debian:11", "debian:10",
            "alpine:3.18", "alpine:3.17",
            "node:18", "node:16", "node:20",
            "python:3.11", "python:3.10", "python:3.9",
            "openjdk:17", "openjdk:11",
            "nginx:alpine", "nginx:latest"
        );
        
        return ResponseEntity.ok(images);
    }

    @GetMapping("/stats")
    public ResponseEntity<ContainerStatsResponse> getUserStats(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        List<ContainerDTO> containers = containerService.getUserContainers(userPrincipal.getUsername());
        
        long running = containers.stream().filter(ContainerDTO::isRunning).count();
        long total = containers.size();
        long stopped = total - running;
        
        ContainerStatsResponse stats = new ContainerStatsResponse(total, running, stopped);
        
        return ResponseEntity.ok(stats);
    }

    // Inner class for stats response
    public static class ContainerStatsResponse {
        private long total;
        private long running;
        private long stopped;

        public ContainerStatsResponse(long total, long running, long stopped) {
            this.total = total;
            this.running = running;
            this.stopped = stopped;
        }

        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }

        public long getRunning() { return running; }
        public void setRunning(long running) { this.running = running; }

        public long getStopped() { return stopped; }
        public void setStopped(long stopped) { this.stopped = stopped; }
    }
}