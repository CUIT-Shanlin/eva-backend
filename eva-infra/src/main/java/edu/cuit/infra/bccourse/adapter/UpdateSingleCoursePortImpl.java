package edu.cuit.infra.bccourse.adapter;

import edu.cuit.bc.course.application.port.UpdateSingleCoursePort;
import edu.cuit.client.dto.cmd.course.UpdateSingleCourseCmd;
import edu.cuit.domain.gateway.course.CourseUpdateGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * bc-course：修改单节课端口适配器（复用既有 CourseUpdateGateway，保持行为不变）。
 */
@Component
@RequiredArgsConstructor
public class UpdateSingleCoursePortImpl implements UpdateSingleCoursePort {
    private final CourseUpdateGateway courseUpdateGateway;

    @Override
    public Map<String, Map<Integer, Integer>> updateSingleCourse(
            String userName,
            Integer semId,
            UpdateSingleCourseCmd updateSingleCourseCmd
    ) {
        return courseUpdateGateway.updateSingleCourse(userName, semId, updateSingleCourseCmd);
    }
}
