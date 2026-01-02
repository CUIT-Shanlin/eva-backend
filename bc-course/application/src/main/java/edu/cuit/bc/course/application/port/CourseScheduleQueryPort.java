package edu.cuit.bc.course.application.port;

import edu.cuit.client.dto.clientobject.course.SingleCourseCO;
import edu.cuit.client.dto.query.CourseQuery;

import java.util.List;

/**
 * 课程读侧查询端口（渐进式重构：先以“委托既有查询实现”为主，保持行为不变）。
 */
public interface CourseScheduleQueryPort {
    List<List<Integer>> getWeekCourses(Integer semId, Integer week);

    List<SingleCourseCO> getPeriodInfo(Integer semId, CourseQuery courseQuery);

    String getDate(Integer semId, Integer week, Integer day);
}

