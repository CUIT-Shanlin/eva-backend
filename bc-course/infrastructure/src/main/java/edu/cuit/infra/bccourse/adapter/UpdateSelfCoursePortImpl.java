package edu.cuit.infra.bccourse.adapter;

import edu.cuit.bc.course.application.port.UpdateSelfCoursePort;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeInfoCO;
import edu.cuit.domain.gateway.course.CourseUpdateGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * bc-course：教师自助改课端口适配器（复用既有 CourseUpdateGateway，保持行为不变）。
 */
@Component
@RequiredArgsConstructor
public class UpdateSelfCoursePortImpl implements UpdateSelfCoursePort {
    private final CourseUpdateGateway courseUpdateGateway;

    @Override
    public Map<String, Map<Integer, Integer>> updateSelfCourse(String userName, SelfTeachCourseCO selfTeachCourseCO, List<SelfTeachCourseTimeInfoCO> timeList) {
        return courseUpdateGateway.updateSelfCourse(userName, selfTeachCourseCO, timeList);
    }
}

