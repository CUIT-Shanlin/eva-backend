package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.port.DeleteCoursePort;

import java.util.Map;
import java.util.Objects;

/**
 * 课程写侧：删除单门课程入口用例（保持行为不变：不在入口用例层新增校验/异常转换）。
 */
public class DeleteCourseEntryUseCase {
    private final DeleteCoursePort port;

    public DeleteCourseEntryUseCase(DeleteCoursePort port) {
        this.port = Objects.requireNonNull(port, "port");
    }

    public Map<String, Map<Integer, Integer>> deleteCourse(Integer semId, Integer id) {
        return port.deleteCourse(semId, id);
    }
}
