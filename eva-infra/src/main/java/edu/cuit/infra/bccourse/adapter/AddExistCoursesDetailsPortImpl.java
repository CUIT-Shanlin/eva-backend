package edu.cuit.infra.bccourse.adapter;

import edu.cuit.bc.course.application.port.AddExistCoursesDetailsPort;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeCO;
import edu.cuit.domain.gateway.course.CourseUpdateGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * bc-course：批量新建多节课（已有课程）端口适配器（复用既有 CourseUpdateGateway，保持行为不变）。
 */
@Component
@RequiredArgsConstructor
public class AddExistCoursesDetailsPortImpl implements AddExistCoursesDetailsPort {
    private final CourseUpdateGateway courseUpdateGateway;

    @Override
    public void addExistCoursesDetails(Integer courseId, SelfTeachCourseTimeCO timeCO) {
        courseUpdateGateway.addExistCoursesDetails(courseId, timeCO);
    }
}

