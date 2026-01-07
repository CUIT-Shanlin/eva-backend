package edu.cuit.infra.bccourse.adapter;

import edu.cuit.bc.course.application.port.AddNotExistCoursesDetailsPort;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeCO;
import edu.cuit.client.dto.cmd.course.UpdateCourseCmd;
import edu.cuit.domain.gateway.course.CourseUpdateGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * bc-course：批量新建多节课（新课程）端口适配器（复用既有 CourseUpdateGateway，保持行为不变）。
 */
@Component
@RequiredArgsConstructor
public class AddNotExistCoursesDetailsPortImpl implements AddNotExistCoursesDetailsPort {
    private final CourseUpdateGateway courseUpdateGateway;

    @Override
    public void addNotExistCoursesDetails(Integer semId, Integer teacherId, UpdateCourseCmd courseInfo, List<SelfTeachCourseTimeCO> dateArr) {
        courseUpdateGateway.addNotExistCoursesDetails(semId, teacherId, courseInfo, dateArr);
    }
}

