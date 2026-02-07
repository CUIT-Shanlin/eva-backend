package edu.cuit.app.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 通用“事务提交后发布事件”发布器。
 *
 * <p>说明：用于渐进式 DDD 重构阶段的跨模块联动；未来可替换为 MQ + Outbox。</p>
 */
@Component
@RequiredArgsConstructor
public class AfterCommitEventPublisher {
    private final ApplicationEventPublisher publisher;

    public void publishAfterCommit(Object event) {
        if (event == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            publisher.publishEvent(event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publisher.publishEvent(event);
            }
        });
    }
}

