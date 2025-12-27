package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.DeleteCourseTypeCommand;
import edu.cuit.bc.course.application.port.DeleteCourseTypeRepository;

import java.util.Objects;

/**
 * 删除课程类型用例。
 *
 * <p>说明：现阶段只做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class DeleteCourseTypeUseCase {
    private final DeleteCourseTypeRepository repository;

    public DeleteCourseTypeUseCase(DeleteCourseTypeRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public void execute(DeleteCourseTypeCommand command) {
        Objects.requireNonNull(command, "command");
        repository.delete(command.typeIds());
    }
}

