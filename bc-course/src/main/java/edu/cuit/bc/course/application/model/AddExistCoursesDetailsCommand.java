package edu.cuit.bc.course.application.model;

import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeCO;

/**
 * 批量新建多节课（已有课程）命令（写模型输入）。
 *
 * <p>说明：渐进式重构阶段沿用旧系统 DTO，以保证行为不变。</p>
 */
public record AddExistCoursesDetailsCommand(
        Integer courseId,
        SelfTeachCourseTimeCO timeCO
) { }

