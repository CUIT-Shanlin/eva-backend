package edu.cuit.bc.course.application.model;

import edu.cuit.client.dto.data.course.CourseType;

/**
 * 新增课程类型命令（写模型输入）。
 *
 * <p>说明：渐进式重构阶段沿用旧系统 DTO，以保证行为不变。</p>
 */
public record AddCourseTypeCommand(
        CourseType courseType
) { }

