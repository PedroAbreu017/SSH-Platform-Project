// WebSocketConfig.java - adicionar no topo
/**
 * WebSocket Configuration for Real SSH Terminal
 * 
 * STATUS: Work in Progress - Not fully functional
 * TODO: Fix WebSocket handshake issues and SSH session management
 * 
 * Current implementation has issues:
 * - WebSocket connection unstable (error 1006)
 * - SSH session termination issues
 * - Requires additional debugging for production use
 * 
 * For now, frontend uses simulated terminal.
 * This serves as foundation for v2.0 real SSH implementation.
 
package com.platform.service;

import com.platform.model.entity.Container;
import com.platform.websocket.TerminalSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

@Service
public class TerminalService {

    private static final Logger logger = LoggerFactory.getLogger(TerminalService.class);
    
    @Autowired
    private ContainerService containerService;

    public TerminalSession createTerminalSession(String containerId, Consumer<String> outputCallback) throws Exception {
        // Get container info
        Container container = containerService.getContainerById(Long.parseLong(containerId));
        
        if (container == null) {
            throw new IllegalArgumentException("Container not found: " + containerId);
        }
        
        // Create SSH connection to container
        String sshCommand = String.format("ssh -p %d root@localhost", container.getSshPort());
        
        logger.info("Creating terminal session with command: {}", sshCommand);
        
        ProcessBuilder pb = new ProcessBuilder("ssh", "-p", String.valueOf(container.getSshPort()), 
                                             "-o", "StrictHostKeyChecking=no",
                                             "-o", "UserKnownHostsFile=/dev/null",
                                             "root@localhost");
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        
        return new TerminalSession(process, outputCallback);
    }
}
    // WebSocketConfig.java - adicionar no topo
/**
 * WebSocket Configuration for Real SSH Terminal
 * 
 * STATUS: Work in Progress - Not fully functional
 * TODO: Fix WebSocket handshake issues and SSH session management
 * 
 * Current implementation has issues:
 * - WebSocket connection unstable (error 1006)
 * - SSH session termination issues
 * - Requires additional debugging for production use
 * 
 * For now, frontend uses simulated terminal.
 * This serves as foundation for v2.0 real SSH implementation.
 */