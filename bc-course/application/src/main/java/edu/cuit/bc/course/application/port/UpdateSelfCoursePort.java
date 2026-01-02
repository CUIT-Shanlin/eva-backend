package edu.cuit.bc.course.application.port;

import edu.cuit.client.dto.clientobject.course.SelfTeachCourseCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeInfoCO;

import java.util.List;
import java.util.Map;

/**
 * 课程写侧：教师自助改课端口（渐进式重构：委托既有 legacy gateway，保持行为不变）。
 */
public interface UpdateSelfCoursePort {
    Map<String, Map<Integer, Integer>> updateSelfCourse(String userName, SelfTeachCourseCO selfTeachCourseCO, List<SelfTeachCourseTimeInfoCO> timeList);
}

