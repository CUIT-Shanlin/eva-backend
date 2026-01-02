package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.port.UpdateSingleCoursePort;
import edu.cuit.client.dto.cmd.course.UpdateSingleCourseCmd;

import java.util.Map;
import java.util.Objects;

/**
 * 课程写侧：修改单节课入口用例（保持行为不变：不在用例层新增校验/异常转换）。
 */
public class UpdateSingleCourseEntryUseCase {
    private final UpdateSingleCoursePort port;

    public UpdateSingleCourseEntryUseCase(UpdateSingleCoursePort port) {
        this.port = Objects.requireNonNull(port, "port");
    }

    public Map<String, Map<Integer, Integer>> updateSingleCourse(
            String userName,
            Integer semId,
            UpdateSingleCourseCmd updateSingleCourseCmd
    ) {
        return port.updateSingleCourse(userName, semId, updateSingleCourseCmd);
    }
}

