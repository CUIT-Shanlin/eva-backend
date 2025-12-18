package edu.cuit.bc.messaging.application.port;

import java.util.Map;

/**
 * 给老师发送“任务类消息”的端口（单体阶段复用 MsgResult.sendMsgtoTeacher）。
 */
public interface TeacherTaskMessagePort {
    void sendToTeacher(Map<String, Map<Integer, Integer>> messageMap, Integer operatorUserId);
}

