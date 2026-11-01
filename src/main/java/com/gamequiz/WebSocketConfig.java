package com.gamequiz;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Serwer będzie wysyłał wiadomości do klientów na kanały zaczynające się od /topic
        config.enableSimpleBroker("/topic");
        // Klient (frontend) będzie wysyłał wiadomości do serwera na adresy zaczynające się od /app
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Punkt wejścia dla WebSocketów, z którego skorzysta nasz frontend
        registry.addEndpoint("/ws-quiz").setAllowedOriginPatterns("*").withSockJS();
    }
}
