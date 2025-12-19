package edu.cuit.app.event.listener;

import edu.cuit.app.service.impl.MsgServiceImpl;
import edu.cuit.bc.evaluation.domain.event.EvaluationTaskPostedEvent;
import edu.cuit.client.bo.MessageBO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 发布评教任务后：发送该任务的待办评教消息。
 *
 * <p>这是跨限界上下文的联动逻辑：单体阶段放在 listener；拆服务后可迁移到 bc-messaging 的订阅者。</p>
 */
@Component
@RequiredArgsConstructor
public class EvaluationTaskPostedMessageSenderListener {
    private final MsgServiceImpl msgService;

    @EventListener
    public void on(EvaluationTaskPostedEvent event) {
        msgService.sendMessage(new MessageBO().setMsg("")
                .setMode(1).setIsShowName(1)
                .setRecipientId(event.evaluatorId()).setSenderId(event.evaluatorId())
                .setType(0).setTaskId(event.taskId()));
    }
}

