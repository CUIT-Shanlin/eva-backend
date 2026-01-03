package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.AddCourseTypeCommand;
import edu.cuit.client.dto.data.course.CourseType;

import java.util.Objects;

/**
 * 课程写侧：新增课程类型入口用例（保持行为不变：不在入口用例层新增校验/异常转换）。
 */
public class AddCourseTypeEntryUseCase {
    private final AddCourseTypeUseCase useCase;

    public AddCourseTypeEntryUseCase(AddCourseTypeUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "useCase");
    }

    public void addCourseType(CourseType courseType) {
        useCase.execute(new AddCourseTypeCommand(courseType));
    }
}
