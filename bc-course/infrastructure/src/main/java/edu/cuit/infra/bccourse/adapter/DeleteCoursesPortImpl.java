package edu.cuit.infra.bccourse.adapter;

import edu.cuit.bc.course.application.port.DeleteCoursesPort;
import edu.cuit.client.dto.data.course.CoursePeriod;
import edu.cuit.domain.gateway.course.CourseDeleteGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * bc-course：批量删除某节课端口适配器（复用既有 CourseDeleteGateway，保持行为不变）。
 */
@Component
@RequiredArgsConstructor
public class DeleteCoursesPortImpl implements DeleteCoursesPort {
    private final CourseDeleteGateway courseDeleteGateway;

    @Override
    public Map<String, Map<Integer, Integer>> deleteCourses(Integer semId, Integer id, CoursePeriod coursePeriod) {
        return courseDeleteGateway.deleteCourses(semId, id, coursePeriod);
    }
}

