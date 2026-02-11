package edu.cuit.app.config;

import edu.cuit.app.websocket.MessageChannel;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(new MessageChannel(),"/send/msg")
                .addInterceptors(new WebSocketInterceptor())
                .setAllowedOriginPatterns("*");
    }
}
