package edu.cuit.infra.bccourse.adapter;

import edu.cuit.bc.course.application.port.ImportCourseFilePort;
import edu.cuit.client.bo.CourseExcelBO;
import edu.cuit.client.dto.clientobject.SemesterCO;
import edu.cuit.domain.gateway.course.CourseUpdateGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * bc-course：导入课表文件端口适配器（复用既有 CourseUpdateGateway，保持行为不变）。
 */
@Component
@RequiredArgsConstructor
public class ImportCourseFilePortImpl implements ImportCourseFilePort {
    private final CourseUpdateGateway courseUpdateGateway;

    @Override
    public Map<String, Map<Integer, Integer>> importCourseFile(Map<String, List<CourseExcelBO>> courseExce, SemesterCO semester, Integer type) {
        return courseUpdateGateway.importCourseFile(courseExce, semester, type);
    }
}

