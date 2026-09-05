package com.flowforge.websockets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of WebSocket sessions, scoped by tenant.
 * Lifecycle:
 *  subscribe(tenantId, session)   <- called when client connects
 *  unsubscribe(tenantId, session) <- called when client disconnects
 *  getSessions(tenantId)          <- called when broadcasting an event
 */
@Component
public class WebSocketConnectionManager {

    private final Logger log = LoggerFactory.getLogger(WebSocketConnectionManager.class);
    private final ConcurrentHashMap<String, Set<WebSocketSession>> subscriptions = new ConcurrentHashMap<>();

    public void subscribe(String tenantId, WebSocketSession session) {
        subscriptions.computeIfAbsent(tenantId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.debug("[WebSocket] Subscribed session={} tenantId={}", session.getId(), tenantId);
    }

    public void unsubscribe(String tenantId, WebSocketSession session) {
        Set<WebSocketSession> sessions = subscriptions.get(tenantId);
        if(sessions != null) {
            boolean removed = sessions.remove(session);
            if(removed && sessions.isEmpty()) {
                subscriptions.remove(tenantId);
            }
            log.debug("[WebSocket] Unsubscribed session={} tenantId={}", session.getId(), tenantId);
        }
    }

    public List<WebSocketSession> getSessions(String tenantId) {
        Set<WebSocketSession> sessions = subscriptions.get(tenantId);
        return sessions != null ? List.copyOf(sessions) : List.of();
    }

    public int activeConnections() {
        return subscriptions.values().stream().mapToInt(Set::size).sum();
    }




}
