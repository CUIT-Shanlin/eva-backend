package edu.cuit.bc.course.application.model;

import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeCO;
import edu.cuit.client.dto.cmd.course.UpdateCourseCmd;

import java.util.List;

/**
 * 批量新建多节课（新课程）命令（写模型输入）。
 *
 * <p>说明：渐进式重构阶段沿用旧系统 DTO，以保证行为不变。</p>
 */
public record AddNotExistCoursesDetailsCommand(
        Integer semesterId,
        Integer teacherId,
        UpdateCourseCmd courseInfo,
        List<SelfTeachCourseTimeCO> dateArr
) { }

