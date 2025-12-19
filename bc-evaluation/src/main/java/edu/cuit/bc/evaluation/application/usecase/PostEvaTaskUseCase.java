package edu.cuit.bc.evaluation.application.usecase;

import edu.cuit.bc.evaluation.application.model.PostEvaTaskCommand;
import edu.cuit.bc.evaluation.application.port.DomainEventPublisher;
import edu.cuit.bc.evaluation.application.port.PostEvaTaskRepository;
import edu.cuit.bc.evaluation.domain.event.EvaluationTaskPostedEvent;

import java.util.Objects;

/**
 * 发布评教任务用例（写模型入口）。
 *
 * <p>该用例只表达编排，不依赖数据库/框架。</p>
 */
public class PostEvaTaskUseCase {
    private final PostEvaTaskRepository repository;
    private final DomainEventPublisher eventPublisher;

    public PostEvaTaskUseCase(PostEvaTaskRepository repository, DomainEventPublisher eventPublisher) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
    }

    public Integer post(PostEvaTaskCommand command, Integer maxBeEvaNum) {
        Integer taskId = repository.create(command, maxBeEvaNum);
        if (taskId != null) {
            eventPublisher.publishAfterCommit(new EvaluationTaskPostedEvent(taskId, command.evaluatorId()));
        }
        return taskId;
    }
}

