package edu.cuit.bc.course.application.model;

/**
 * 连带删除一门课程命令（写模型输入）。
 */
public record DeleteCourseCommand(
        Integer semesterId,
        Integer courseId
) { }

