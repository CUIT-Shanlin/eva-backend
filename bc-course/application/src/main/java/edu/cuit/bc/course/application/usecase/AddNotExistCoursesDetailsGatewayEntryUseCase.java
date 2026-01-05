package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.AddNotExistCoursesDetailsCommand;
import edu.cuit.client.dto.cmd.course.UpdateCourseCmd;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeCO;

import java.util.List;
import java.util.Objects;

/**
 * 课程写侧：批量新建多节课（新建课程明细）入口用例（保持行为不变：不在入口用例层新增校验/异常转换）。
 *
 * <p>说明：用于让旧 gateway 退化为“仅事务边界 + 委托调用”的壳。</p>
 */
public class AddNotExistCoursesDetailsGatewayEntryUseCase {
    private final AddNotExistCoursesDetailsUseCase useCase;

    public AddNotExistCoursesDetailsGatewayEntryUseCase(AddNotExistCoursesDetailsUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "useCase");
    }

    public void addNotExistCoursesDetails(
            Integer semId,
            Integer teacherId,
            UpdateCourseCmd courseInfo,
            List<SelfTeachCourseTimeCO> dateArr
    ) {
        useCase.execute(new AddNotExistCoursesDetailsCommand(semId, teacherId, courseInfo, dateArr));
    }
}
