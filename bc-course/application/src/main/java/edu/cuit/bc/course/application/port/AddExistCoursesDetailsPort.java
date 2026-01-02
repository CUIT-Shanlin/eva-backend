package edu.cuit.bc.course.application.port;

import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeCO;

/**
 * 课程写侧：批量新建多节课（已有课程）端口（渐进式重构：委托既有 legacy gateway，保持行为不变）。
 */
public interface AddExistCoursesDetailsPort {
    void addExistCoursesDetails(Integer courseId, SelfTeachCourseTimeCO timeCO);
}

