package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.DeleteCourseCommand;
import edu.cuit.bc.course.application.port.DeleteCourseRepository;

import java.util.Map;
import java.util.Objects;

/**
 * 连带删除一门课程用例。
 *
 * <p>说明：现阶段只做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class DeleteCourseUseCase {
    private final DeleteCourseRepository repository;

    public DeleteCourseUseCase(DeleteCourseRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public Map<String, Map<Integer, Integer>> execute(DeleteCourseCommand command) {
        Objects.requireNonNull(command, "command");
        return repository.delete(command.semesterId(), command.courseId());
    }
}

