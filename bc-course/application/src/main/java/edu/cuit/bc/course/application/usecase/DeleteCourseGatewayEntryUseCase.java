package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.DeleteCourseCommand;

import java.util.Map;
import java.util.Objects;

/**
 * 课程写侧：删课（连带删除一门课程）入口用例（保持行为不变：不在入口用例层新增校验/异常转换）。
 *
 * <p>说明：用于让旧 gateway 退化为“仅事务边界 + 委托调用”的壳。</p>
 */
public class DeleteCourseGatewayEntryUseCase {
    private final DeleteCourseUseCase useCase;

    public DeleteCourseGatewayEntryUseCase(DeleteCourseUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "useCase");
    }

    public Map<String, Map<Integer, Integer>> deleteCourse(Integer semId, Integer id) {
        return useCase.execute(new DeleteCourseCommand(semId, id));
    }
}
