package com.tizhen.app.websocket.battle;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 注册对战 WebSocket。握手来源限制仅用于拦截跨站页面发起的连接，
 * 真正的身份校验仍由 Spring Session 完成。
 */
@Configuration
@EnableWebSocket
public class BattleWebSocketConfig implements WebSocketConfigurer {

    private final BattleWebSocketHandler handler;
    private final BattleHandshakeInterceptor handshakeInterceptor;
    private final String[] allowedOriginPatterns;

    public BattleWebSocketConfig(
            BattleWebSocketHandler handler,
            BattleHandshakeInterceptor handshakeInterceptor,
            @Value("${tizhen.battle.websocket.allowed-origin-patterns:"
                    + "http://localhost:*,http://127.0.0.1:*}")
            String[] allowedOriginPatterns) {
        this.handler = handler;
        this.handshakeInterceptor = handshakeInterceptor;
        this.allowedOriginPatterns = allowedOriginPatterns;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/battle/rooms/**")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns(allowedOriginPatterns);
    }
}
