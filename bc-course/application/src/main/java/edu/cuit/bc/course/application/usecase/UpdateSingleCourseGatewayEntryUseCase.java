package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.model.UpdateSingleCourseCommand;
import edu.cuit.client.dto.cmd.course.UpdateSingleCourseCmd;

import java.util.Map;
import java.util.Objects;

/**
 * 课程写侧：修改单节课入口用例（保持行为不变：不在入口用例层新增校验/异常转换）。
 *
 * <p>说明：用于让旧 gateway 退化为“仅事务边界 + 异常转换 + 委托调用”的壳。</p>
 */
public class UpdateSingleCourseGatewayEntryUseCase {
    private final UpdateSingleCourseUseCase useCase;

    public UpdateSingleCourseGatewayEntryUseCase(UpdateSingleCourseUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "useCase");
    }

    public Map<String, Map<Integer, Integer>> updateSingleCourse(
            String userName,
            Integer semId,
            UpdateSingleCourseCmd updateSingleCourseCmd
    ) {
        return useCase.execute(new UpdateSingleCourseCommand(
                semId,
                updateSingleCourseCmd.getId(),
                updateSingleCourseCmd.getTime() == null ? null : updateSingleCourseCmd.getTime().getWeek(),
                updateSingleCourseCmd.getTime() == null ? null : updateSingleCourseCmd.getTime().getDay(),
                updateSingleCourseCmd.getTime() == null ? null : updateSingleCourseCmd.getTime().getStartTime(),
                updateSingleCourseCmd.getTime() == null ? null : updateSingleCourseCmd.getTime().getEndTime(),
                updateSingleCourseCmd.getLocation()
        ));
    }
}
