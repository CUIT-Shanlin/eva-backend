package edu.cuit.bc.course.application.model;

/**
 * 改课（修改单节课课次信息）命令（写模型输入）。
 */
public record UpdateSingleCourseCommand(
        Integer semesterId,
        Integer courInfId,
        Integer week,
        Integer day,
        Integer startTime,
        Integer endTime,
        String location
) { }

