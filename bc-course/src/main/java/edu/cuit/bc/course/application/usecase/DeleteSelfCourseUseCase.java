package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.DeleteSelfCourseCommand;
import edu.cuit.bc.course.application.port.DeleteSelfCourseRepository;

import java.util.Map;
import java.util.Objects;

/**
 * 教师自助删课用例。
 *
 * <p>说明：现阶段只做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class DeleteSelfCourseUseCase {
    private final DeleteSelfCourseRepository repository;

    public DeleteSelfCourseUseCase(DeleteSelfCourseRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public Map<String, Map<Integer, Integer>> execute(DeleteSelfCourseCommand command) {
        Objects.requireNonNull(command, "command");
        return repository.delete(command.username(), command.courseId());
    }
}

