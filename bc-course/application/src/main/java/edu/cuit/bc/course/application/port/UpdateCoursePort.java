package edu.cuit.bc.course.application.port;

import edu.cuit.client.dto.cmd.course.UpdateCourseCmd;

import java.util.Map;

/**
 * 课程写侧：修改课程信息端口（渐进式重构：委托既有 legacy gateway，保持行为不变）。
 */
public interface UpdateCoursePort {
    Map<String, Map<Integer, Integer>> updateCourse(Integer semId, UpdateCourseCmd updateCourseCmd);
}

