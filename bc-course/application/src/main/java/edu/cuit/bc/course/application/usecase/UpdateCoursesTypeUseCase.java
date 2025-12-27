package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.UpdateCoursesTypeCommand;
import edu.cuit.bc.course.application.port.UpdateCoursesTypeRepository;

import java.util.Objects;

/**
 * 批量修改课程对应类型用例。
 *
 * <p>说明：现阶段仅做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class UpdateCoursesTypeUseCase {
    private final UpdateCoursesTypeRepository repository;

    public UpdateCoursesTypeUseCase(UpdateCoursesTypeRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public void execute(UpdateCoursesTypeCommand command) {
        Objects.requireNonNull(command, "command");
        repository.update(command);
    }
}

