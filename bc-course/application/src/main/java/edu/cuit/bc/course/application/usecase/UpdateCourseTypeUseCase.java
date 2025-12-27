package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.UpdateCourseTypeCommand;
import edu.cuit.bc.course.application.port.UpdateCourseTypeRepository;

import java.util.Objects;

/**
 * 修改一个课程类型用例。
 *
 * <p>说明：现阶段仅做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class UpdateCourseTypeUseCase {
    private final UpdateCourseTypeRepository repository;

    public UpdateCourseTypeUseCase(UpdateCourseTypeRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public void execute(UpdateCourseTypeCommand command) {
        Objects.requireNonNull(command, "command");
        repository.update(command);
    }
}

