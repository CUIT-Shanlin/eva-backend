package edu.cuit.infra.bccourse.adapter;

import edu.cuit.bc.course.application.port.UpdateCoursePort;
import edu.cuit.client.dto.cmd.course.UpdateCourseCmd;
import edu.cuit.domain.gateway.course.CourseUpdateGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * bc-course：修改课程信息端口适配器（复用既有 CourseUpdateGateway，保持行为不变）。
 */
@Component
@RequiredArgsConstructor
public class UpdateCoursePortImpl implements UpdateCoursePort {
    private final CourseUpdateGateway courseUpdateGateway;

    @Override
    public Map<String, Map<Integer, Integer>> updateCourse(Integer semId, UpdateCourseCmd updateCourseCmd) {
        return courseUpdateGateway.updateCourse(semId, updateCourseCmd);
    }
}

