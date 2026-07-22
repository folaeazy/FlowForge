package com.flowforge.websockets;

import com.flowforge.api.dto.ActivityFeedDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

/**
 * Sends ActivityFeedDto messages to all WebSocket sessions for a tenant.
 *
 * WHY a separate class (not just in the listener):
 * Broadcasting logic — converting DTO to JSON, iterating sessions, handling
 * errors — is a distinct concern from event listening. If the broadcaster
 * ever needs to queue messages for reliability, or log metrics on sent/failed
 * messages, it's isolated here rather than entangled with the listener.
 */
@Component
public class ActivityFeedBroadcaster {
    private final Logger log = LoggerFactory.getLogger(ActivityFeedBroadcaster.class);
    private final WebSocketConnectionManager webSocketConnectionManager;
    private final ObjectMapper objectMapper;

    public ActivityFeedBroadcaster(WebSocketConnectionManager webSocketConnectionManager, ObjectMapper objectMapper) {
        this.webSocketConnectionManager = webSocketConnectionManager;
        this.objectMapper = objectMapper;
    }

    /**
     * Serialize DTO to JSON and send to all sessions subscribed to tenantId.
     * Silently drops messages if delivery fails (WebSocket is unreliable at
     * the application level anyway — broken connections are expected).
     */
    public void broadcast(String tenantId, ActivityFeedDto activityFeedDto) {
        List<WebSocketSession> sessions = webSocketConnectionManager.getSessions(tenantId);

        if (sessions.isEmpty()) {
            log.debug("[Broadcast] No active sessions for tenant={}", tenantId);
            return;
        }

        try{
            String json = objectMapper.writeValueAsString(activityFeedDto);
            TextMessage message = new TextMessage(json);

            for(WebSocketSession session :  sessions) {
                try {
                    if(session.isOpen()) {
                        session.sendMessage(message);
                    }
                } catch (IOException e) {
                    log.warn("[Broadcast] Failed to send to session={}: {}", session.getId(), e.getMessage());
                }
            }
            log.debug("[Broadcast] Sent to {} sessions for tenant={}", sessions.size(), tenantId);
        } catch (Exception e) {
            log.error("[Broadcast] Failed to serialize/send dto for tenant={}", tenantId, e);
        }
    }
}
