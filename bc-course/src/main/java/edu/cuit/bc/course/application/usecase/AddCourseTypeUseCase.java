package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.AddCourseTypeCommand;
import edu.cuit.bc.course.application.port.AddCourseTypeRepository;

import java.util.Objects;

/**
 * 新增课程类型用例。
 *
 * <p>说明：现阶段只做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class AddCourseTypeUseCase {
    private final AddCourseTypeRepository repository;

    public AddCourseTypeUseCase(AddCourseTypeRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public void execute(AddCourseTypeCommand command) {
        Objects.requireNonNull(command, "command");
        repository.add(command.courseType());
    }
}

