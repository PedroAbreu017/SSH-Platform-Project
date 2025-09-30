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
 

package com.platform.config;

import com.platform.websocket.TerminalWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private TerminalWebSocketHandler terminalWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        System.out.println("REGISTERING WEBSOCKET HANDLERS - SIMPLE VERSION");

        registry.addHandler(terminalWebSocketHandler, "/ws/terminal")
                .setAllowedOrigins("*");

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