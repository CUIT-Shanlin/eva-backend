package edu.cuit.app.bcmessaging.adapter;

import edu.cuit.app.service.operate.course.MsgResult;
import edu.cuit.bc.messaging.application.port.CourseBroadcastPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * bc-messaging：课程广播端口适配器（复用现有 MsgResult）。
 */
@Component
@RequiredArgsConstructor
public class CourseBroadcastPortAdapter implements CourseBroadcastPort {
    private final MsgResult msgResult;

    @Override
    public void sendToAll(Map<String, Map<Integer, Integer>> messageMap, Integer operatorUserId) {
        msgResult.SendMsgToAll(messageMap, operatorUserId);
    }

    @Override
    public void sendNormal(Map<String, Map<Integer, Integer>> messageMap, Integer operatorUserId) {
        msgResult.toNormalMsg(messageMap, operatorUserId);
    }
}

