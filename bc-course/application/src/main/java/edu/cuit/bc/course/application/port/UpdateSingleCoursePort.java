package edu.cuit.bc.course.application.port;

import edu.cuit.client.dto.cmd.course.UpdateSingleCourseCmd;

import java.util.Map;

/**
 * 课程写侧：修改单节课端口（渐进式重构：委托既有 legacy gateway，保持行为不变）。
 */
public interface UpdateSingleCoursePort {
    Map<String, Map<Integer, Integer>> updateSingleCourse(
            String userName,
            Integer semId,
            UpdateSingleCourseCmd updateSingleCourseCmd
    );
}

