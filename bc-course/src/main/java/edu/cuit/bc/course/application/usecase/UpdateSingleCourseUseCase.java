package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.UpdateSingleCourseCommand;
import edu.cuit.bc.course.application.port.UpdateSingleCourseRepository;
import edu.cuit.bc.course.domain.UpdateSingleCourseException;

import java.util.Map;
import java.util.Objects;

/**
 * 改课用例（修改单节课课次信息）。
 */
public class UpdateSingleCourseUseCase {
    private final UpdateSingleCourseRepository repository;

    public UpdateSingleCourseUseCase(UpdateSingleCourseRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public Map<String, Map<Integer, Integer>> execute(UpdateSingleCourseCommand command) {
        Objects.requireNonNull(command, "command");

        if (command.semesterId() == null) {
            throw new UpdateSingleCourseException("学期ID不能为空");
        }
        if (command.courInfId() == null) {
            throw new UpdateSingleCourseException("课程详情id不能为空");
        }
        if (command.week() == null || command.day() == null || command.startTime() == null || command.endTime() == null) {
            throw new UpdateSingleCourseException("课程时间不能为空");
        }
        if (command.location() == null || command.location().isBlank()) {
            throw new UpdateSingleCourseException("授课教室不能为空");
        }

        return repository.update(command);
    }
}

