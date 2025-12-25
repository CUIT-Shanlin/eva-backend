package edu.cuit.bc.evaluation.application.usecase;

import edu.cuit.bc.evaluation.application.model.SubmitEvaluationCommand;
import edu.cuit.bc.evaluation.application.model.SubmitEvaluationContext;
import edu.cuit.bc.evaluation.application.port.DomainEventPublisher;
import edu.cuit.bc.evaluation.application.port.SubmitEvaluationRepository;
import edu.cuit.bc.evaluation.domain.SubmitEvaluationException;
import edu.cuit.bc.evaluation.domain.TaskStatus;
import edu.cuit.bc.evaluation.domain.event.EvaluationSubmittedEvent;

import java.util.Objects;

/**
 * 提交评教用例（写模型入口）。
 *
 * <p>该用例只表达业务规则与编排，不依赖数据库/框架。</p>
 */
public class SubmitEvaluationUseCase {
    private final SubmitEvaluationRepository repository;
    private final DomainEventPublisher eventPublisher;

    public SubmitEvaluationUseCase(SubmitEvaluationRepository repository, DomainEventPublisher eventPublisher) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
    }

    public void submit(SubmitEvaluationCommand command) {
        if (command == null || command.taskId() == null) {
            throw new SubmitEvaluationException("评教任务不能为空");
        }
        if (command.textValue() == null || command.textValue().isBlank()) {
            throw new SubmitEvaluationException("文本信息不能为空");
        }

        SubmitEvaluationContext context = repository.loadContext(command.taskId());
        if (context == null) {
            throw new SubmitEvaluationException("该任务不存在");
        }
        if (context.taskStatus() == null) {
            throw new SubmitEvaluationException("任务状态异常，无法提交");
        }
        if (context.taskStatus() != TaskStatus.PENDING) {
            throw new SubmitEvaluationException("该任务已完成或已撤回，不能提交");
        }
        if (context.courInfId() == null || context.courseId() == null) {
            throw new SubmitEvaluationException("该任务对应的课程信息不存在，不能提交");
        }

        repository.saveEvaluation(context, command.textValue(), command.formPropsValues());
        eventPublisher.publishAfterCommit(new EvaluationSubmittedEvent(
                context.taskId(),
                context.evaluatorId(),
                context.courseId(),
                context.semesterId()
        ));
    }
}

