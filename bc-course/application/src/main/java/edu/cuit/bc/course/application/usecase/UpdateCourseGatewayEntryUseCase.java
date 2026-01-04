package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.UpdateCourseInfoCommand;
import edu.cuit.client.dto.cmd.course.UpdateCourseCmd;

import java.util.Map;
import java.util.Objects;

/**
 * 课程写侧：修改课程信息入口用例（保持行为不变：不在入口用例层新增校验/异常转换）。
 *
 * <p>说明：用于让旧 gateway 退化为“仅事务边界 + 异常转换 + 委托调用”的壳。</p>
 */
public class UpdateCourseGatewayEntryUseCase {
    private final UpdateCourseInfoUseCase useCase;

    public UpdateCourseGatewayEntryUseCase(UpdateCourseInfoUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "useCase");
    }

    public Map<String, Map<Integer, Integer>> updateCourse(Integer semId, UpdateCourseCmd updateCourseCmd) {
        return useCase.execute(new UpdateCourseInfoCommand(semId, updateCourseCmd));
    }
}
