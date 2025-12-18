package edu.cuit.app.bcmessaging.adapter;

import edu.cuit.app.service.operate.course.MsgResult;
import edu.cuit.bc.messaging.application.port.TeacherTaskMessagePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * bc-messaging：老师任务消息端口适配器（复用现有 MsgResult.sendMsgtoTeacher）。
 */
@Component
@RequiredArgsConstructor
public class TeacherTaskMessagePortAdapter implements TeacherTaskMessagePort {
    private final MsgResult msgResult;

    @Override
    public void sendToTeacher(Map<String, Map<Integer, Integer>> messageMap, Integer operatorUserId) {
        msgResult.sendMsgtoTeacher(messageMap, operatorUserId);
    }
}

