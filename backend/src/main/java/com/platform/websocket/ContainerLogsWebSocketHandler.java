package com.platform.websocket;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.Frame;
import com.platform.model.entity.Container;
import com.platform.repository.ContainerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ContainerLogsWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ContainerLogsWebSocketHandler.class);
    
    private final DockerClient dockerClient;
    private final ContainerRepository containerRepository;
    private final Map<String, ResultCallback.Adapter<Frame>> activeCallbacks = new ConcurrentHashMap<>();

    public ContainerLogsWebSocketHandler(DockerClient dockerClient, ContainerRepository containerRepository) {
        this.dockerClient = dockerClient;
        this.containerRepository = containerRepository;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("WebSocket connection established: {}", session.getId());
        
        // Extract containerId from URI path
        String path = session.getUri().getPath();
        String containerIdStr = path.substring(path.lastIndexOf('/') + 1);
        
        try {
            Long containerId = Long.parseLong(containerIdStr);
            Container container = containerRepository.findById(containerId)
                    .orElseThrow(() -> new RuntimeException("Container not found"));

            if (!container.isRunning()) {
                session.sendMessage(new TextMessage("ERROR: Container is not running"));
                session.close(CloseStatus.NORMAL);
                return;
            }

            // Start streaming logs
            startLogStreaming(session, container.getContainerId());
            
        } catch (NumberFormatException e) {
            logger.error("Invalid container ID format: {}", containerIdStr);
            session.sendMessage(new TextMessage("ERROR: Invalid container ID"));
            session.close(CloseStatus.BAD_DATA);
        } catch (Exception e) {
            logger.error("Error establishing log stream", e);
            session.sendMessage(new TextMessage("ERROR: " + e.getMessage()));
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private void startLogStreaming(WebSocketSession session, String dockerContainerId) {
        LogContainerCmd logCmd = dockerClient.logContainerCmd(dockerContainerId)
                .withStdOut(true)
                .withStdErr(true)
                .withFollowStream(true)
                .withTailAll();

        ResultCallback.Adapter<Frame> callback = new ResultCallback.Adapter<>() {
            @Override
            public void onNext(Frame frame) {
                try {
                    String logLine = new String(frame.getPayload()).trim();
                    if (!logLine.isEmpty() && session.isOpen()) {
                        session.sendMessage(new TextMessage(logLine));
                    }
                } catch (IOException e) {
                    logger.error("Error sending log message to WebSocket", e);
                    try {
                        session.close(CloseStatus.SERVER_ERROR);
                    } catch (IOException ex) {
                        logger.error("Error closing session", ex);
                    }
                }
            }

            @Override
            public void onError(Throwable throwable) {
                logger.error("Error in log stream for session {}", session.getId(), throwable);
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage("ERROR: " + throwable.getMessage()));
                        session.close(CloseStatus.SERVER_ERROR);
                    }
                } catch (IOException e) {
                    logger.error("Error closing session after error", e);
                }
            }

            @Override
            public void onComplete() {
                logger.info("Log stream completed for session {}", session.getId());
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage("STREAM_COMPLETE"));
                        session.close(CloseStatus.NORMAL);
                    }
                } catch (IOException e) {
                    logger.error("Error closing session on completion", e);
                }
            }
        };

        activeCallbacks.put(session.getId(), callback);
        logCmd.exec(callback);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        logger.info("WebSocket connection closed: {} with status: {}", session.getId(), status);
        
        ResultCallback.Adapter<Frame> callback = activeCallbacks.remove(session.getId());
        if (callback != null) {
            try {
                callback.close();
            } catch (IOException e) {
                logger.error("Error closing Docker log callback", e);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        logger.error("WebSocket transport error for session: {}", session.getId(), exception);
        afterConnectionClosed(session, CloseStatus.SERVER_ERROR);
    }
}