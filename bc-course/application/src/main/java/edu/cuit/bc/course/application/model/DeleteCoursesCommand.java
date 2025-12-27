package edu.cuit.bc.course.application.model;

import edu.cuit.client.dto.data.course.CoursePeriod;

/**
 * 批量删除某节课命令（写模型输入）。
 */
public record DeleteCoursesCommand(
        Integer semesterId,
        Integer courInfId,
        CoursePeriod coursePeriod
) { }
