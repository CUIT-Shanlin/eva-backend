package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.AddExistCoursesDetailsCommand;
import edu.cuit.bc.course.application.port.AddExistCoursesDetailsRepository;

import java.util.Objects;

/**
 * 批量新建多节课（已有课程）用例。
 *
 * <p>说明：现阶段只做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class AddExistCoursesDetailsUseCase {
    private final AddExistCoursesDetailsRepository repository;

    public AddExistCoursesDetailsUseCase(AddExistCoursesDetailsRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public void execute(AddExistCoursesDetailsCommand command) {
        Objects.requireNonNull(command, "command");
        repository.add(command.courseId(), command.timeCO());
    }
}

