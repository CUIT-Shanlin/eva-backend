package edu.cuit.bc.course.application.port;

import edu.cuit.client.dto.clientobject.course.RecommendCourseCO;
import edu.cuit.client.dto.query.condition.MobileCourseQuery;

import java.util.List;

/**
 * 课程读侧：指定时间段课程查询端口（渐进式重构：先委托既有查询实现，保持行为不变）。
 */
public interface TimeCourseQueryPort {
    List<RecommendCourseCO> getTimeCourse(Integer semId, MobileCourseQuery courseQuery, String userName);
}

