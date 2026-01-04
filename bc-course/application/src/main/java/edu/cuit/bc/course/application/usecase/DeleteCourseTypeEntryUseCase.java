package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.DeleteCourseTypeCommand;

import java.util.List;
import java.util.Objects;

/**
 * 课程写侧：删除课程类型入口用例（保持行为不变：不在入口用例层新增校验/异常转换）。
 */
public class DeleteCourseTypeEntryUseCase {
    private final DeleteCourseTypeUseCase useCase;

    public DeleteCourseTypeEntryUseCase(DeleteCourseTypeUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "useCase");
    }

    public void deleteCourseType(List<Integer> ids) {
        useCase.execute(new DeleteCourseTypeCommand(ids));
    }
}
