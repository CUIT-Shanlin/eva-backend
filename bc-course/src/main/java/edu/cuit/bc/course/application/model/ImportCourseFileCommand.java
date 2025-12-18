package edu.cuit.bc.course.application.model;

import edu.cuit.client.bo.CourseExcelBO;
import edu.cuit.client.dto.clientobject.SemesterCO;

import java.util.List;
import java.util.Map;

/**
 * 课表导入命令（写模型输入）。
 *
 * <p>说明：处于渐进式重构阶段，为“行为不变”暂沿用旧系统的 DTO 结构。</p>
 */
public record ImportCourseFileCommand(
        Map<String, List<CourseExcelBO>> courseExce,
        SemesterCO semester,
        Integer type
) { }

