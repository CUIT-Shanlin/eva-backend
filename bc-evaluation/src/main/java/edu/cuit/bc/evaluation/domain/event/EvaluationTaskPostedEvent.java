package edu.cuit.bc.evaluation.domain.event;

import java.time.Instant;

/**
 * 评教任务发布事件（生成新的待执行任务）。
 */
public record EvaluationTaskPostedEvent(
        Integer taskId,
        Integer evaluatorId,
        Instant occurredAt
) implements DomainEvent {
    public EvaluationTaskPostedEvent(Integer taskId, Integer evaluatorId) {
        this(taskId, evaluatorId, Instant.now());
    }
}

