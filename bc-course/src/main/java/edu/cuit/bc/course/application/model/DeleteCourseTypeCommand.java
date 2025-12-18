package edu.cuit.bc.course.application.model;

import java.util.List;

/**
 * 删除课程类型命令（写模型输入）。
 */
public record DeleteCourseTypeCommand(
        List<Integer> typeIds
) { }

