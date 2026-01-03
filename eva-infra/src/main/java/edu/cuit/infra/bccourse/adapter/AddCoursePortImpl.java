package edu.cuit.infra.bccourse.adapter;

import edu.cuit.bc.course.application.port.AddCoursePort;
import edu.cuit.domain.gateway.course.CourseUpdateGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * bc-course：新增课程端口适配器（复用既有 CourseUpdateGateway，保持行为不变）。
 */
@Component
@RequiredArgsConstructor
public class AddCoursePortImpl implements AddCoursePort {
    private final CourseUpdateGateway courseUpdateGateway;

    @Override
    public void addCourse(Integer semId) {
        courseUpdateGateway.addCourse(semId);
    }
}
