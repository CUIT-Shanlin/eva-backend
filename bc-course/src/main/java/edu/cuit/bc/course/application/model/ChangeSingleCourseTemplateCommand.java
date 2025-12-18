package edu.cuit.bc.course.application.model;

/**
 * 单课程模板切换命令（写模型输入）。
 */
public record ChangeSingleCourseTemplateCommand(
        Integer semesterId,
        Integer courseId,
        Integer templateId
) { }

