package com.flowforge.websockets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

public class ActivityFeedHandler implements WebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ActivityFeedHandler.class);

    @Autowired
    private WebSocketConnectionManager connectionManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String tenantId = extractTenantId(session);
        log.info("[WebSocket] Connection established: session={} tenantId={}", session.getId(), tenantId);
        connectionManager.subscribe(tenantId, session);

    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        // Activity Feed is server → client only. Clients shouldn't send us anything.
        // If they do, log it but ignore it.
        log.debug("[WebSocket] Received unexpected message on session={}: {}",
                session.getId(), message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String tenantId = extractTenantId(session);
        log.error("[WebSocket] Transport error: session={} tenantId={}",
                session.getId(), tenantId, exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String tenantId = extractTenantId(session);
        log.info("[WebSocket] Connection closed: session={} tenantId={} status={}",
                session.getId(), tenantId, closeStatus.getCode());
        connectionManager.unsubscribe(tenantId, session);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }


    /**
     * Extract tenantId from the URL path.
     * WebSocket URL: /ws/activity-feed/{tenantId}
     * session.getUri() returns the full URI, we parse it.
     *
     * In production, you'd validate this against the authenticated user's
     * tenant (Phase 7 Security). For now, just extract and trust.
     */
    private String extractTenantId(WebSocketSession session) {
        String path = session.getUri().getPath();
        // path is like "/ws/activity-feed/tenant-A"
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : "unknown";
    }
}
