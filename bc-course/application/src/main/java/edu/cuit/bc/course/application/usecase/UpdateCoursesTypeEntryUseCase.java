package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.UpdateCoursesTypeCommand;
import edu.cuit.client.dto.cmd.course.UpdateCoursesToTypeCmd;

import java.util.Objects;

/**
 * 课程写侧：批量课程类型修改入口用例（保持行为不变：不在入口用例层新增校验/异常转换）。
 */
public class UpdateCoursesTypeEntryUseCase {
    private final UpdateCoursesTypeUseCase useCase;

    public UpdateCoursesTypeEntryUseCase(UpdateCoursesTypeUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "useCase");
    }

    public void updateCoursesType(UpdateCoursesToTypeCmd updateCoursesToTypeCmd) {
        useCase.execute(new UpdateCoursesTypeCommand(updateCoursesToTypeCmd));
    }
}
