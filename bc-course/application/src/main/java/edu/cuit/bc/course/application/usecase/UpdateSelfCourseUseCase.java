package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.UpdateSelfCourseCommand;
import edu.cuit.bc.course.application.port.UpdateSelfCourseRepository;

import java.util.Map;
import java.util.Objects;

/**
 * 教师自助改课用例。
 *
 * <p>说明：现阶段只做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class UpdateSelfCourseUseCase {
    private final UpdateSelfCourseRepository repository;

    public UpdateSelfCourseUseCase(UpdateSelfCourseRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public Map<String, Map<Integer, Integer>> execute(UpdateSelfCourseCommand command) {
        Objects.requireNonNull(command, "command");
        return repository.update(command.username(), command.selfTeachCourseCO(), command.timeList());
    }
}

