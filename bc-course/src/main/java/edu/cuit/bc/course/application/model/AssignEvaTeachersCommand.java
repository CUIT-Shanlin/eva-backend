package edu.cuit.bc.course.application.model;

import java.util.List;

/**
 * 分配听课/评教老师命令（写模型输入）。
 */
public record AssignEvaTeachersCommand(
        Integer semesterId,
        Integer courInfId,
        List<Integer> evaTeacherIdList
) { }

