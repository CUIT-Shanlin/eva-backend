package edu.cuit.bc.course.application.model;

import edu.cuit.client.dto.cmd.course.UpdateCourseTypeCmd;

/**
 * 修改一个课程类型命令（写模型输入）。
 *
 * <p>说明：渐进式重构阶段沿用旧系统 DTO，以保证行为不变。</p>
 */
public record UpdateCourseTypeCommand(
        UpdateCourseTypeCmd updateCourseTypeCmd
) { }

