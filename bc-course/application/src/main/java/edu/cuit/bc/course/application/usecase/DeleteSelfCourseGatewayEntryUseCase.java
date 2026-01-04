package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.DeleteSelfCourseCommand;

import java.util.Map;
import java.util.Objects;

/**
 * 课程写侧：教师自助删课入口用例（保持行为不变：不在入口用例层新增校验/异常转换）。
 *
 * <p>说明：用于让旧 gateway 退化为“仅事务边界 + 委托调用”的壳。</p>
 */
public class DeleteSelfCourseGatewayEntryUseCase {
    private final DeleteSelfCourseUseCase useCase;

    public DeleteSelfCourseGatewayEntryUseCase(DeleteSelfCourseUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "useCase");
    }

    public Map<String, Map<Integer, Integer>> deleteSelfCourse(String userName, Integer courseId) {
        return useCase.execute(new DeleteSelfCourseCommand(userName, courseId));
    }
}
