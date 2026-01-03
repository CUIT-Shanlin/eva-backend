package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.port.AddCoursePort;

import java.util.Objects;

/**
 * 课程写侧：新增课程入口用例（保持行为不变：不在入口用例层新增校验/异常转换）。
 */
public class AddCourseEntryUseCase {
    private final AddCoursePort port;

    public AddCourseEntryUseCase(AddCoursePort port) {
        this.port = Objects.requireNonNull(port, "port");
    }

    public void addCourse(Integer semId) {
        port.addCourse(semId);
    }
}
