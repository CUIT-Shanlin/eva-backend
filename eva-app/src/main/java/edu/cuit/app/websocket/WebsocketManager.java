package edu.cuit.app.websocket;

import com.alibaba.cola.exception.SysException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Websocket 集中管理
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebsocketManager {

    private final Map<Object, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final Map<Integer,Pair<MessageConsumer,Class<?>>> consumers = new HashMap<>();

    private final ObjectMapper objectMapper;

    private final Executor executor;

    private int consumerIndex = 0;

    /**
     * 注册session
     * @param loginId 用户名
     * @param session 会话
     */
    public void registerSession(Object loginId,WebSocketSession session) {
        sessions.putIfAbsent(loginId,new HashSet<>());
        sessions.get(loginId).add(session);
    }

    /**
     * 注销session
     * @param loginId 用户名
     * @param session 会话
     */
    public void unregisterSession(Object loginId,WebSocketSession session) {
        sessions.get(loginId).remove(session);
    }

    /**
     * 向某用户发送消息（将发送到该用户的所有websocket session）
     * message已经做序列化处理
     * @param loginId 用户名
     * @param message 消息对象
     */
    public void sendMessage(Object loginId,Object message) {
        for (WebSocketSession webSocketSession : sessions.get(loginId)) {
            try {
                webSocketSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            } catch (IOException e) {
                log.error("websocket处理对象失败，请联系管理员",e);
                throw new SysException("websocket处理对象失败，请联系管理员");
            }
        }
    }

    /**
     * 广播消息，将消息发给所有在线用户
     * @param message 消息对象
     */
    public void broadcastMessage(Object message) {
        CompletableFuture.runAsync(() -> {
            for (Object loginId : sessions.keySet()) {
                sendMessage(loginId,message);
            }
        },executor);
    }

    /**
     * 注册消息监听器，收到消息时触发
     * @param targetClass 序列化为目标对象
     * @param consumer 消息消费者
     * @return 监听器分配的id
     */
    public int registerListener(Class<?> targetClass,MessageConsumer consumer) {
        consumers.put(consumerIndex++,Pair.of(consumer,targetClass));
        return consumerIndex - 1;
    }

    /**
     * 注销监听器
     * @param listenerId 监听器id
     */
    public void unregisterListener(Integer listenerId) {
        consumers.remove(listenerId);
    }

    /**
     * 触发事件
     */
    public void invokeEvent(WebSocketSession session,String message) {
        Object loginId = session.getAttributes().get("loginId");
        for (Pair<MessageConsumer, Class<?>> consumer : consumers.values()) {
            Class<?> targetClass = consumer.getRight();
            try {
                Object target = objectMapper.readValue(message, targetClass);
                consumer.getLeft().apply(loginId,session,target);
            } catch (JsonProcessingException e) {
                log.error("websocket处理对象失败，请联系管理员",e);
                throw new SysException("websocket处理对象失败，请联系管理员");
            }
        }
    }

    @FunctionalInterface
    public interface MessageConsumer {
        void apply(Object loginId, WebSocketSession session, Object message);
    }

}
