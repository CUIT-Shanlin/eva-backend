package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.DeleteCoursesCommand;
import edu.cuit.bc.course.application.port.DeleteCoursesRepository;

import java.util.Map;
import java.util.Objects;

/**
 * 批量删除某节课用例。
 *
 * <p>说明：现阶段只做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class DeleteCoursesUseCase {
    private final DeleteCoursesRepository repository;

    public DeleteCoursesUseCase(DeleteCoursesRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public Map<String, Map<Integer, Integer>> execute(DeleteCoursesCommand command) {
        Objects.requireNonNull(command, "command");
        return repository.delete(command.semesterId(), command.courInfId(), command.coursePeriod());
    }
}

