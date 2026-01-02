package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.port.AddExistCoursesDetailsPort;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeCO;

import java.util.Objects;

/**
 * 课程写侧：批量新建多节课（已有课程）入口用例（保持行为不变：不在用例层新增校验/异常转换）。
 */
public class AddExistCoursesDetailsEntryUseCase {
    private final AddExistCoursesDetailsPort port;

    public AddExistCoursesDetailsEntryUseCase(AddExistCoursesDetailsPort port) {
        this.port = Objects.requireNonNull(port, "port");
    }

    public void addExistCoursesDetails(Integer courseId, SelfTeachCourseTimeCO timeCO) {
        port.addExistCoursesDetails(courseId, timeCO);
    }
}

