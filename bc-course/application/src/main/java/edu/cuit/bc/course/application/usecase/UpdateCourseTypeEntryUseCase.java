package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.UpdateCourseTypeCommand;
import edu.cuit.client.dto.cmd.course.UpdateCourseTypeCmd;

import java.util.Objects;

/**
 * 课程写侧：课程类型修改入口用例（保持行为不变：不在入口用例层新增校验/异常转换）。
 */
public class UpdateCourseTypeEntryUseCase {
    private final UpdateCourseTypeUseCase useCase;

    public UpdateCourseTypeEntryUseCase(UpdateCourseTypeUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "useCase");
    }

    public void updateCourseType(UpdateCourseTypeCmd courseType) {
        useCase.execute(new UpdateCourseTypeCommand(courseType));
    }
}
