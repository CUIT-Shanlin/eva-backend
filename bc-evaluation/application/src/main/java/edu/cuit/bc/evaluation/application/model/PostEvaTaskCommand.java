package edu.cuit.bc.evaluation.application.model;

/**
 * 发布评教任务命令（写侧入口参数）。
 *
 * <p>该命令不依赖 Web DTO，避免 BC 被外部模型污染。</p>
 */
public record PostEvaTaskCommand(
        Integer courInfId,
        Integer evaluatorId
) {
}

