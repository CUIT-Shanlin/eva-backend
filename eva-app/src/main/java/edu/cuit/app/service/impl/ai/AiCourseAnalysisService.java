package edu.cuit.app.service.impl.ai;

import edu.cuit.app.aop.CheckSemId;
import edu.cuit.client.api.ai.IAiCourseAnalysisService;
import edu.cuit.client.api.course.IUserCourseService;
import edu.cuit.client.bo.ai.AiAnalysisBO;
import edu.cuit.client.dto.clientobject.course.CourseDetailCO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiCourseAnalysisService implements IAiCourseAnalysisService {

    private final IUserCourseService userCourseService;

    @Override
    @CheckSemId
    public AiAnalysisBO analysis(Integer semId, Integer teacherId) {
        List<CourseDetailCO> courseDetailsList = userCourseService.getUserCourseDetail(teacherId, semId);

        return null;
    }
}
