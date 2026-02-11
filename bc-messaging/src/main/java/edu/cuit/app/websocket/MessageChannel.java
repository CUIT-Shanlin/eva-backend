package edu.cuit.app.websocket;

import cn.hutool.extra.spring.SpringUtil;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 消息相关ws端口
 */
public class MessageChannel extends TextWebSocketHandler {

    private final WebsocketManager websocketManager;

    {
        websocketManager = SpringUtil.getBean(WebsocketManager.class);
    }

    // 收到消息
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        websocketManager.invokeEvent(session,message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Object loginId = session.getAttributes().get("loginId");
        websocketManager.unregisterSession(loginId,session);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Object loginId = session.getAttributes().get("loginId");
        websocketManager.registerSession(loginId,session);
    }
}
