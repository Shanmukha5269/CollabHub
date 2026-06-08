package com.collabHub.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.AbstractSubProtocolEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Component
@Slf4j
public class WebSocketConnectListener {

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.getAccessor(event.getMessage(), SimpMessageHeaderAccessor.class);
        if (accessor != null && accessor.getSessionId() != null) {
            log.info("STOMP Client connected: sessionId={}", accessor.getSessionId());
        } else {
            log.info("STOMP Client connected");
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.getAccessor(event.getMessage(), SimpMessageHeaderAccessor.class);
        if (accessor != null && accessor.getSessionId() != null) {
            log.info("STOMP Client disconnected: sessionId={}", accessor.getSessionId());
        } else {
            log.info("STOMP Client disconnected");
        }
    }

    @EventListener
    public void handleSubscriptionEvent(SessionSubscribeEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.getAccessor(event.getMessage(), SimpMessageHeaderAccessor.class);
        String sessionId = accessor != null ? accessor.getSessionId() : "unknown";
        String destination = event.getMessage() != null ? event.getMessage().getHeaders().get("destination", String.class) : "unknown";
        
        log.info("STOMP Client subscribed: sessionId={}, destination={}", 
                sessionId, destination);
    }

    @EventListener
    public void handleUnsubscribeEvent(SessionUnsubscribeEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.getAccessor(event.getMessage(), SimpMessageHeaderAccessor.class);
        if (accessor != null) {
            log.info("STOMP Client unsubscribed: sessionId={}", accessor.getSessionId());
        } else {
            log.info("STOMP Client unsubscribed");
        }
    }

    @EventListener
    public void handleMessageEvent(AbstractSubProtocolEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.getAccessor(event.getMessage(), SimpMessageHeaderAccessor.class);
        if (accessor != null) {
            log.debug("STOMP Message handled: sessionId={}", accessor.getSessionId());
        } else {
            log.debug("STOMP Message handled");
        }
    }
}