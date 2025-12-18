package edu.cuit.bc.messaging.application.port;

import java.util.Map;

/**
 * 课程相关消息广播端口（单体阶段由现有 MsgResult 适配实现）。
 */
public interface CourseBroadcastPort {
    void sendToAll(Map<String, Map<Integer, Integer>> messageMap, Integer operatorUserId);

    void sendNormal(Map<String, Map<Integer, Integer>> messageMap, Integer operatorUserId);

    /**
     * 发送“携带 taskId 的任务类消息”（历史上对应 MsgResult.toSendMsg）。
     *
     * <p>说明：该能力用于兼容旧链路中“消息需要关联 taskId”的场景，避免行为变化。</p>
     */
    void sendTaskLinked(Map<String, Map<Integer, Integer>> messageMap, Integer operatorUserId);
}
