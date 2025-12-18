package edu.cuit.bc.course.application.model;

import edu.cuit.client.dto.cmd.course.UpdateCoursesToTypeCmd;

/**
 * 批量修改课程对应类型命令（写模型输入）。
 *
 * <p>说明：渐进式重构阶段沿用旧系统 DTO，以保证行为不变。</p>
 */
public record UpdateCoursesTypeCommand(
        UpdateCoursesToTypeCmd updateCoursesToTypeCmd
) { }

