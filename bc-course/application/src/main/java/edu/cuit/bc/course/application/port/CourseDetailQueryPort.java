package edu.cuit.bc.course.application.port;

import edu.cuit.client.dto.clientobject.course.SingleCourseDetailCO;

import java.util.Optional;

/**
 * 课程读侧：单节课详情查询端口（渐进式重构：复用既有查询实现，保持行为不变）。
 */
public interface CourseDetailQueryPort {
    Optional<SingleCourseDetailCO> getCourseDetail(Integer semId, Integer id);
}

