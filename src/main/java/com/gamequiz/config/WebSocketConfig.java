package com.gamequiz.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over SockJS. The server only pushes: clients subscribe to {@code /topic/game/{code}/...} and send their
 * commands through the REST API, which keeps authorization (host token) in one place.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final QuizProperties properties;

    public WebSocketConfig(QuizProperties properties) {
        this.properties = properties;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = properties.getAllowedOrigins().toArray(new String[0]);
        registry.addEndpoint("/ws-quiz").setAllowedOriginPatterns(origins).withSockJS();
    }
}
