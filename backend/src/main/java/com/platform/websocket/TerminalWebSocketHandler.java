// src/main/java/com/platform/websocket/TerminalWebSocketHandler.java
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

import com.platform.service.ContainerService;
import com.platform.service.TerminalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TerminalWebSocketHandler implements WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(TerminalWebSocketHandler.class);
    
    @Autowired
    private TerminalService terminalService;
    
    @Autowired
    private ContainerService containerService;
    
    // Store active sessions
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TerminalSession> terminalSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String containerId = extractContainerIdFromQuery(session.getUri());
        String sessionId = session.getId();
        
        // Extrair username do JWT token
        String username = extractUsernameFromSession(session);
        if (username == null) {
            session.sendMessage(new TextMessage("\r\n\u001b[31mAuthentication required\u001b[0m\r\n"));
            session.close();
            return;
        }
        
        logger.info("Terminal WebSocket connection established for container: {} user: {} session: {}", 
                    containerId, username, sessionId);
        
        sessions.put(sessionId, session);
        
        try {
            // Create terminal session for container
            TerminalSession terminalSession = terminalService.createTerminalSession(containerId, 
                (data) -> {
                    // Callback to send data to WebSocket client
                    try {
                        if (session.isOpen()) {
                            session.sendMessage(new TextMessage(data));
                        }
                    } catch (IOException e) {
                        logger.error("Failed to send terminal data to WebSocket", e);
                    }
                }
            );
            
            terminalSessions.put(sessionId, terminalSession);
            
            // Send initial prompt
            session.sendMessage(new TextMessage("\r\n\u001b[32mConnected to container " + containerId + "\u001b[0m\r\n"));
            
        } catch (Exception e) {
            logger.error("Failed to create terminal session for container: {}", containerId, e);
            session.sendMessage(new TextMessage("\r\n\u001b[31mError: Failed to connect to container\u001b[0m\r\n"));
            session.close();
        }
    }
    
    private String extractUsernameFromSession(WebSocketSession session) {
        try {
            // Pegar token dos query parameters
            String token = null;
            
            // Tentar pegar do query string
            String query = session.getUri().getQuery();
            if (query != null && query.contains("token=")) {
                token = query.substring(query.indexOf("token=") + 6);
                if (token.contains("&")) {
                    token = token.substring(0, token.indexOf("&"));
                }
            }
            
            if (token != null) {
                // Decodificar JWT (método simples)
                String[] parts = token.split("\\.");
                if (parts.length >= 2) {
                    String payload = new String(java.util.Base64.getDecoder().decode(parts[1]));
                    // Extrair username do JSON (método simples)
                    if (payload.contains("\"sub\":\"")) {
                        int start = payload.indexOf("\"sub\":\"") + 7;
                        int end = payload.indexOf("\"", start);
                        return payload.substring(start, end);
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.error("Failed to extract username from session", e);
            return null;
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String sessionId = session.getId();
        TerminalSession terminalSession = terminalSessions.get(sessionId);
        
        if (terminalSession != null && message instanceof TextMessage) {
            String input = ((TextMessage) message).getPayload();
            logger.debug("Terminal input from session {}: {}", sessionId, input);
            
            // Send input to terminal session
            terminalSession.sendInput(input);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("Terminal WebSocket transport error for session: {}", session.getId(), exception);
        cleanupSession(session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        logger.info("Terminal WebSocket connection closed for session: {} status: {}", session.getId(), closeStatus);
        cleanupSession(session.getId());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
    
    private void cleanupSession(String sessionId) {
        sessions.remove(sessionId);
        TerminalSession terminalSession = terminalSessions.remove(sessionId);
        if (terminalSession != null) {
            try {
                terminalSession.close();
            } catch (Exception e) {
                logger.error("Error closing terminal session", e);
            }
        }
    }

    private String extractContainerIdFromQuery(URI uri) {
    String query = uri.getQuery();
    if (query != null && query.contains("containerId=")) {
        String containerId = query.substring(query.indexOf("containerId=") + 12);
        if (containerId.contains("&")) {
            containerId = containerId.substring(0, containerId.indexOf("&"));
        }
        return containerId;
    }
    return "1"; // default
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