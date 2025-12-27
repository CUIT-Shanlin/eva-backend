package edu.cuit.bc.course.application.model;

import edu.cuit.client.dto.clientobject.course.SelfTeachCourseCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeInfoCO;

import java.util.List;

/**
 * 教师自助改课命令（写模型输入）。
 *
 * <p>说明：渐进式重构阶段沿用旧系统 DTO，以保证行为不变。</p>
 */
public record UpdateSelfCourseCommand(
        String username,
        SelfTeachCourseCO selfTeachCourseCO,
        List<SelfTeachCourseTimeInfoCO> timeList
) { }

