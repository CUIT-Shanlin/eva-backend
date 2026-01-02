package edu.cuit.infra.bccourse.adapter;

import edu.cuit.bc.course.application.port.CourseScheduleQueryPort;
import edu.cuit.client.dto.clientobject.course.SingleCourseCO;
import edu.cuit.client.dto.query.CourseQuery;
import edu.cuit.domain.gateway.course.CourseQueryGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * bc-course：课程读侧查询端口适配器（复用既有 CourseQueryGateway，保持行为不变）。
 */
@Component
@RequiredArgsConstructor
public class CourseScheduleQueryPortImpl implements CourseScheduleQueryPort {
    private final CourseQueryGateway courseQueryGateway;

    @Override
    public List<List<Integer>> getWeekCourses(Integer semId, Integer week) {
        return courseQueryGateway.getWeekCourses(semId, week);
    }

    @Override
    public List<SingleCourseCO> getPeriodInfo(Integer semId, CourseQuery courseQuery) {
        return courseQueryGateway.getPeriodInfo(semId, courseQuery);
    }

    @Override
    public String getDate(Integer semId, Integer week, Integer day) {
        return courseQueryGateway.getDate(semId, week, day);
    }
}

