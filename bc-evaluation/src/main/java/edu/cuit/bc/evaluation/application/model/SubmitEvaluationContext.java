package edu.cuit.bc.evaluation.application.model;

import edu.cuit.bc.evaluation.domain.TaskStatus;

/**
 * 提交评教用例需要的上下文快照（从基础设施层装配）。
 *
 * <p>强调：这是用例所需的最小信息集合，不试图一比一映射数据库表。</p>
 */
public record SubmitEvaluationContext(
        Integer taskId,
        Integer evaluatorId,
        String evaluatorName,
        Integer courInfId,
        Integer courseId,
        Integer semesterId,
        Integer templateId,
        String templateName,
        String templateDescription,
        String templateProps,
        TaskStatus taskStatus
) { }

