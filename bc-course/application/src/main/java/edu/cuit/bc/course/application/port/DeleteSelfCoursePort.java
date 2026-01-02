package edu.cuit.bc.course.application.port;

import java.util.Map;

/**
 * 课程写侧：教师自助删课端口（渐进式重构：委托既有 legacy gateway，保持行为不变）。
 */
public interface DeleteSelfCoursePort {
    Map<String, Map<Integer, Integer>> deleteSelfCourse(String userName, Integer courseId);
}

