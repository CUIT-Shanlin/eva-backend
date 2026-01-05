package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.AddExistCoursesDetailsCommand;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeCO;

import java.util.Objects;

/**
 * 课程写侧：批量新建多节课（已有课程）入口用例（保持行为不变：不在入口用例层新增校验/异常转换）。
 *
 * <p>说明：用于让旧 gateway 退化为“仅事务边界 + 委托调用”的壳。</p>
 */
public class AddExistCoursesDetailsGatewayEntryUseCase {
    private final AddExistCoursesDetailsUseCase useCase;

    public AddExistCoursesDetailsGatewayEntryUseCase(AddExistCoursesDetailsUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "useCase");
    }

    public void addExistCoursesDetails(Integer courseId, SelfTeachCourseTimeCO timeCO) {
        useCase.execute(new AddExistCoursesDetailsCommand(courseId, timeCO));
    }
}
