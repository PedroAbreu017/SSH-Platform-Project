// src/main/java/com/platform/websocket/TerminalSession.java
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
 
package com.platform.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class TerminalSession implements AutoCloseable {
    
    private static final Logger logger = LoggerFactory.getLogger(TerminalSession.class);
    
    private final Process process;
    private final OutputStream processInput;
    private final Consumer<String> outputCallback;
    private final ExecutorService executor;
    private volatile boolean closed = false;

    public TerminalSession(Process process, Consumer<String> outputCallback) {
        this.process = process;
        this.processInput = process.getOutputStream();
        this.outputCallback = outputCallback;
        this.executor = Executors.newFixedThreadPool(2);
        
        // Start reading process output
        startOutputReader();
    }
    
    private void startOutputReader() {
        executor.submit(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                
                char[] buffer = new char[1024];
                int len;
                
                while (!closed && (len = reader.read(buffer)) != -1) {
                    String output = new String(buffer, 0, len);
                    outputCallback.accept(output);
                }
                
            } catch (IOException e) {
                if (!closed) {
                    logger.error("Error reading terminal output", e);
                    outputCallback.accept("\r\n\u001b[31mTerminal session ended unexpectedly\u001b[0m\r\n");
                }
            }
        });
    }
    
    public void sendInput(String input) throws IOException {
        if (!closed && processInput != null) {
            processInput.write(input.getBytes(StandardCharsets.UTF_8));
            processInput.flush();
        }
    }
    
    @Override
    public void close() throws Exception {
        closed = true;
        
        if (processInput != null) {
            processInput.close();
        }
        
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }
        
        executor.shutdown();
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