package edu.cuit.bc.course.application.port;

import java.util.Map;

/**
 * 课程写侧：删除单门课程端口（渐进式重构：委托既有 legacy gateway，保持行为不变）。
 */
public interface DeleteCoursePort {
    Map<String, Map<Integer, Integer>> deleteCourse(Integer semId, Integer id);
}
