package com.platform.config;

import com.platform.websocket.ContainerLogsWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ContainerLogsWebSocketHandler logsHandler;

    public WebSocketConfig(ContainerLogsWebSocketHandler logsHandler) {
        this.logsHandler = logsHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(logsHandler, "/ws/logs/{containerId}")
                .setAllowedOrigins("http://localhost:5173", "http://localhost:3000");
              
    }
}