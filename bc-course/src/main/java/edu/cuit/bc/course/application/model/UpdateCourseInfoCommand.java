package edu.cuit.bc.course.application.model;

import edu.cuit.client.dto.cmd.course.UpdateCourseCmd;

/**
 * 修改课程信息命令（写模型输入）。
 *
 * <p>说明：渐进式重构阶段沿用旧系统 {@link UpdateCourseCmd}，以保证行为不变。</p>
 */
public record UpdateCourseInfoCommand(
        Integer semesterId,
        UpdateCourseCmd updateCourseCmd
) { }

