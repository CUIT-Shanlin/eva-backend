package edu.cuit.bc.course.application.model;

import java.util.List;

/**
 * 批量切换课程模板命令（写模型输入）。
 */
public record ChangeCourseTemplateCommand(
        Integer semesterId,
        Integer templateId,
        List<Integer> courseIdList
) { }

