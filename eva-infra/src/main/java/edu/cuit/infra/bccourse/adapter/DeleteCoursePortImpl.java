package edu.cuit.infra.bccourse.adapter;

import edu.cuit.bc.course.application.port.DeleteCoursePort;
import edu.cuit.domain.gateway.course.CourseDeleteGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * bc-course：删除单门课程端口适配器（复用既有 CourseDeleteGateway，保持行为不变）。
 */
@Component
@RequiredArgsConstructor
public class DeleteCoursePortImpl implements DeleteCoursePort {
    private final CourseDeleteGateway courseDeleteGateway;

    @Override
    public Map<String, Map<Integer, Integer>> deleteCourse(Integer semId, Integer id) {
        return courseDeleteGateway.deleteCourse(semId, id);
    }
}
