package edu.cuit.app.event;

import edu.cuit.bc.evaluation.application.port.DomainEventPublisher;
import edu.cuit.bc.evaluation.domain.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 使用 Spring 本地事件实现“事务提交后发布领域事件”。\n
 * <p>单体阶段足够；未来可替换为 MQ + Outbox 的实现而不影响领域用例。</p>
 */
@Component
@RequiredArgsConstructor
public class SpringAfterCommitDomainEventPublisher implements DomainEventPublisher {
    private final ApplicationEventPublisher publisher;

    @Override
    public void publishAfterCommit(DomainEvent event) {
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

