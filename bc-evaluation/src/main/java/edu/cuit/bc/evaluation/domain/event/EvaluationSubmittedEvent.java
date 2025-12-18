package edu.cuit.bc.evaluation.domain.event;

import java.time.Instant;

/**
 * 提交评教完成事件（任务从待执行 -> 已执行）。
 */
public record EvaluationSubmittedEvent(
        Integer taskId,
        Integer evaluatorId,
        Integer courseId,
        Integer semesterId,
        Instant occurredAt
) implements DomainEvent {
    public EvaluationSubmittedEvent(Integer taskId, Integer evaluatorId, Integer courseId, Integer semesterId) {
        this(taskId, evaluatorId, courseId, semesterId, Instant.now());
    }
}

