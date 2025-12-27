package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.UpdateCourseInfoCommand;
import edu.cuit.bc.course.application.port.UpdateCourseInfoRepository;
import edu.cuit.bc.course.domain.UpdateCourseInfoException;

import java.util.Map;
import java.util.Objects;

/**
 * 修改课程信息用例（修改课程科目信息、类型、模板等）。
 */
public class UpdateCourseInfoUseCase {
    private final UpdateCourseInfoRepository repository;

    public UpdateCourseInfoUseCase(UpdateCourseInfoRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public Map<String, Map<Integer, Integer>> execute(UpdateCourseInfoCommand command) {
        Objects.requireNonNull(command, "command");

        if (command.semesterId() == null) {
            throw new UpdateCourseInfoException("学期ID不能为空");
        }
        if (command.updateCourseCmd() == null) {
            throw new UpdateCourseInfoException("课程修改信息不能为空");
        }
        if (command.updateCourseCmd().getId() == null) {
            throw new UpdateCourseInfoException("课程ID不能为空");
        }

        return repository.update(command);
    }
}

