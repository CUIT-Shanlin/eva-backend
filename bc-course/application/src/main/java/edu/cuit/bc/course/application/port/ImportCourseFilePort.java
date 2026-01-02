package edu.cuit.bc.course.application.port;

import edu.cuit.client.bo.CourseExcelBO;
import edu.cuit.client.dto.clientobject.SemesterCO;

import java.util.List;
import java.util.Map;

/**
 * 课程写侧：导入课表文件端口（渐进式重构：委托既有 legacy gateway，保持行为不变）。
 */
public interface ImportCourseFilePort {
    Map<String, Map<Integer, Integer>> importCourseFile(Map<String, List<CourseExcelBO>> courseExce, SemesterCO semester, Integer type);
}

