package edu.cuit.bc.course.application.port;

import java.util.Optional;

/**
 * 查询课程当前模板ID的端口（用于判断模板是否发生切换）。
 */
public interface CourseTemplateIdQueryPort {
    Optional<Integer> findTemplateId(Integer semesterId, Integer courseId);
}

