package com.collabHub.channel.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class RawWebSocketHandler extends TextWebSocketHandler {

    // Connected websocket clients
    private final Set<WebSocketSession> sessions =
            ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {

        sessions.add(session);

        log.info("Client connected: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status
    ) {

        sessions.remove(session);

        log.info("Client disconnected: {}", session.getId());
    }

    // Broadcast message to all connected clients
    public void broadcast(String message) throws IOException {

        for (WebSocketSession wsSession : sessions) {

            if (wsSession.isOpen()) {

                wsSession.sendMessage(
                        new TextMessage(message)
                );
            }
        }
    }
}