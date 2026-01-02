package edu.cuit.bc.course.application.port;

import edu.cuit.client.dto.data.course.CoursePeriod;

import java.util.Map;

/**
 * 课程写侧：批量删除某节课端口（渐进式重构：委托既有 legacy gateway，保持行为不变）。
 */
public interface DeleteCoursesPort {
    Map<String, Map<Integer, Integer>> deleteCourses(Integer semId, Integer id, CoursePeriod coursePeriod);
}

