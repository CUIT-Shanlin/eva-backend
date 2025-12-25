package edu.cuit.bc.evaluation.domain.event;

import java.time.Instant;

/**
 * 领域事件基础接口。
 *
 * <p>注意：事件负载应尽量小，只携带业务标识与少量必要快照，便于未来迁移到 MQ + Outbox。</p>
 */
public interface DomainEvent {
    Instant occurredAt();
}

