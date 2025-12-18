package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.AddNotExistCoursesDetailsCommand;
import edu.cuit.bc.course.application.port.AddNotExistCoursesDetailsRepository;

import java.util.Objects;

/**
 * 批量新建多节课（新课程）用例。
 *
 * <p>说明：现阶段只做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class AddNotExistCoursesDetailsUseCase {
    private final AddNotExistCoursesDetailsRepository repository;

    public AddNotExistCoursesDetailsUseCase(AddNotExistCoursesDetailsRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public void execute(AddNotExistCoursesDetailsCommand command) {
        Objects.requireNonNull(command, "command");
        repository.add(command.semesterId(), command.teacherId(), command.courseInfo(), command.dateArr());
    }
}

