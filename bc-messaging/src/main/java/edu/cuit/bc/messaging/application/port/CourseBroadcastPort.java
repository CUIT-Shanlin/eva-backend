package edu.cuit.bc.messaging.application.port;

import java.util.Map;

/**
 * 课程相关消息广播端口（单体阶段由现有 MsgResult 适配实现）。
 */
public interface CourseBroadcastPort {
    void sendToAll(Map<String, Map<Integer, Integer>> messageMap, Integer operatorUserId);

    void sendNormal(Map<String, Map<Integer, Integer>> messageMap, Integer operatorUserId);
}

