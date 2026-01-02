package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.port.UpdateCoursePort;
import edu.cuit.client.dto.cmd.course.UpdateCourseCmd;

import java.util.Map;
import java.util.Objects;

/**
 * 课程写侧：修改课程信息入口用例（保持行为不变：不在用例层新增校验/异常转换）。
 */
public class UpdateCourseEntryUseCase {
    private final UpdateCoursePort port;

    public UpdateCourseEntryUseCase(UpdateCoursePort port) {
        this.port = Objects.requireNonNull(port, "port");
    }

    public Map<String, Map<Integer, Integer>> updateCourse(Integer semId, UpdateCourseCmd updateCourseCmd) {
        return port.updateCourse(semId, updateCourseCmd);
    }
}

