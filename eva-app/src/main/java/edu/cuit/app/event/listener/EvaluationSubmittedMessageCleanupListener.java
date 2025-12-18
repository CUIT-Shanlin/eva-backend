package edu.cuit.app.event.listener;

import edu.cuit.app.service.impl.MsgServiceImpl;
import edu.cuit.bc.evaluation.domain.event.EvaluationSubmittedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 提交评教后：清理该任务相关的两类评教消息（待办 + 系统逾期提醒）。
 *
 * <p>这是跨限界上下文的联动逻辑：单体阶段放在 listener；拆服务后可迁移到 bc-messaging 的订阅者。</p>
 */
@Component
@RequiredArgsConstructor
public class EvaluationSubmittedMessageCleanupListener {
    private final MsgServiceImpl msgService;

    @EventListener
    public void on(EvaluationSubmittedEvent event) {
        msgService.deleteEvaMsg(event.taskId(), null);
    }
}

