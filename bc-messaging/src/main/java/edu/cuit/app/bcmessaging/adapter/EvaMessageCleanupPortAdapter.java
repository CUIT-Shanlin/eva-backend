package edu.cuit.app.bcmessaging.adapter;

import edu.cuit.client.api.IMsgService;
import edu.cuit.bc.messaging.application.port.EvaMessageCleanupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * bc-messaging：评教消息清理端口适配器（复用现有 MsgService.deleteEvaMsg）。
 */
@Component
@RequiredArgsConstructor
public class EvaMessageCleanupPortAdapter implements EvaMessageCleanupPort {
    private final IMsgService msgService;

    @Override
    public void deleteEvaMsg(Integer taskId) {
        msgService.deleteEvaMsg(taskId, null);
    }
}
