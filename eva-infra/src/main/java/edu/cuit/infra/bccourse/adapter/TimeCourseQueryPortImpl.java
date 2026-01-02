package edu.cuit.infra.bccourse.adapter;

import edu.cuit.bc.course.application.port.TimeCourseQueryPort;
import edu.cuit.client.dto.clientobject.course.RecommendCourseCO;
import edu.cuit.client.dto.query.condition.MobileCourseQuery;
import edu.cuit.domain.gateway.course.CourseQueryGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * bc-course：指定时间段课程查询端口适配器（复用既有 CourseQueryGateway，保持行为不变）。
 */
@Component
@RequiredArgsConstructor
public class TimeCourseQueryPortImpl implements TimeCourseQueryPort {
    private final CourseQueryGateway courseQueryGateway;

    @Override
    public List<RecommendCourseCO> getTimeCourse(Integer semId, MobileCourseQuery courseQuery, String userName) {
        return courseQueryGateway.getPeriodCourse(semId, courseQuery, userName);
    }
}

