package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.port.DeleteSelfCoursePort;

import java.util.Map;
import java.util.Objects;

/**
 * 课程写侧：教师自助删课入口用例（保持行为不变：不在用例层新增校验/异常转换）。
 */
public class DeleteSelfCourseEntryUseCase {
    private final DeleteSelfCoursePort port;

    public DeleteSelfCourseEntryUseCase(DeleteSelfCoursePort port) {
        this.port = Objects.requireNonNull(port, "port");
    }

    public Map<String, Map<Integer, Integer>> deleteSelfCourse(String userName, Integer courseId) {
        return port.deleteSelfCourse(userName, courseId);
    }
}

