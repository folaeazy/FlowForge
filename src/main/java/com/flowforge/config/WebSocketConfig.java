package com.flowforge.config;

import com.flowforge.websockets.ActivityFeedHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(activityFeedHandler(), "/ws/activity-feed/{tenantId}")
                .setAllowedOrigins("*") // will be restricted to actual client domain
                .setAllowedOriginPatterns("*");
    }

    @Bean
    public WebSocketHandler activityFeedHandler() {
        return new ActivityFeedHandler();
    }
}
