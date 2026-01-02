package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.port.DeleteCoursesPort;
import edu.cuit.client.dto.data.course.CoursePeriod;

import java.util.Map;
import java.util.Objects;

/**
 * 课程写侧：批量删除某节课用例（保持行为不变：不在用例层新增校验/异常转换）。
 */
public class DeleteCoursesEntryUseCase {
    private final DeleteCoursesPort port;

    public DeleteCoursesEntryUseCase(DeleteCoursesPort port) {
        this.port = Objects.requireNonNull(port, "port");
    }

    public Map<String, Map<Integer, Integer>> deleteCourses(Integer semId, Integer id, CoursePeriod coursePeriod) {
        return port.deleteCourses(semId, id, coursePeriod);
    }
}

