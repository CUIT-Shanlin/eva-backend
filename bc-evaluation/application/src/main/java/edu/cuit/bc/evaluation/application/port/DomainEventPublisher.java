package edu.cuit.bc.evaluation.application.port;

import edu.cuit.bc.evaluation.domain.event.DomainEvent;

/**
 * 领域事件发布端口（由外部实现：单体阶段可用 Spring 事件；微服务阶段可切换 MQ/Outbox）。 
 */
public interface DomainEventPublisher {
    void publishAfterCommit(DomainEvent event);
}

