package edu.cuit.app.bccourse.adapter;

import edu.cuit.app.convertor.course.CourseBizConvertor;
import edu.cuit.bc.course.application.port.CourseDetailQueryPort;
import edu.cuit.client.dto.clientobject.course.SingleCourseDetailCO;
import edu.cuit.client.dto.clientobject.eva.EvaTeacherInfoCO;
import edu.cuit.domain.gateway.course.CourseQueryGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * bc-course：课程详情查询端口适配器（复用既有 CourseQueryGateway + Convertor，保持行为不变）。
 */
@Component
@RequiredArgsConstructor
public class CourseDetailQueryPortImpl implements CourseDetailQueryPort {
    private final CourseQueryGateway courseQueryGateway;
    private final CourseBizConvertor courseConvertor;

    @Override
    public Optional<SingleCourseDetailCO> getCourseDetail(Integer semId, Integer id) {
        List<EvaTeacherInfoCO> evaUsers = courseQueryGateway.getEvaUsers(id);
        if (evaUsers == null) {
            evaUsers = new ArrayList<>();
        }
        List<EvaTeacherInfoCO> finalEvaUsers = evaUsers;
        return courseQueryGateway.getSingleCourseDetail(id, semId)
                .map(courseEntity -> courseConvertor.toSingleCourseDetailCO(courseEntity, finalEvaUsers));
    }
}

