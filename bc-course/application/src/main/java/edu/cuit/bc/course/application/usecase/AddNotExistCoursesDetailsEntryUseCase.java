package edu.cuit.bc.course.application.usecase;

import edu.cuit.bc.course.application.port.AddNotExistCoursesDetailsPort;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeCO;
import edu.cuit.client.dto.cmd.course.UpdateCourseCmd;

import java.util.List;
import java.util.Objects;

/**
 * 课程写侧：批量新建多节课（新课程）入口用例（保持行为不变：不在用例层新增校验/异常转换）。
 */
public class AddNotExistCoursesDetailsEntryUseCase {
    private final AddNotExistCoursesDetailsPort port;

    public AddNotExistCoursesDetailsEntryUseCase(AddNotExistCoursesDetailsPort port) {
        this.port = Objects.requireNonNull(port, "port");
    }

    public void addNotExistCoursesDetails(Integer semId, Integer teacherId, UpdateCourseCmd courseInfo, List<SelfTeachCourseTimeCO> dateArr) {
        port.addNotExistCoursesDetails(semId, teacherId, courseInfo, dateArr);
    }
}

