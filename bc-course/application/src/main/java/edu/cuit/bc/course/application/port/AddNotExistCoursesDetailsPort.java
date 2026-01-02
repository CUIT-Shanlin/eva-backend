package edu.cuit.bc.course.application.port;

import edu.cuit.client.dto.cmd.course.UpdateCourseCmd;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeCO;

import java.util.List;

/**
 * 课程写侧：批量新建多节课（新课程）端口（渐进式重构：委托既有 legacy gateway，保持行为不变）。
 */
public interface AddNotExistCoursesDetailsPort {
    void addNotExistCoursesDetails(Integer semId, Integer teacherId, UpdateCourseCmd courseInfo, List<SelfTeachCourseTimeCO> dateArr);
}

