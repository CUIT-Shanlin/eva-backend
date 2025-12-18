package edu.cuit.bc.course.application.model;

/**
 * 教师自助删课命令（写模型输入）。
 */
public record DeleteSelfCourseCommand(
        String username,
        Integer courseId
) { }

