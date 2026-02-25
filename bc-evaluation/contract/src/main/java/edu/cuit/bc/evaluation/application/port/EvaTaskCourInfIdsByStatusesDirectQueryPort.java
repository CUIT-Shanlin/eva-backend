package edu.cuit.bc.evaluation.application.port;

import java.util.List;

/**
 * 评教任务 courInfId 列表直查端口（读侧）。
 *
 * <p>用于跨 BC 在课程侧等场景中，按任务状态列表查询评教任务对应的 courInfId 列表。</p>
 *
 * <p>约束：实现侧不引入缓存/切面副作用；查询条件、结果顺序与空值语义需保持与旧 Mapper 调用一致；入参为空应为 no-op。</p>
 */
public interface EvaTaskCourInfIdsByStatusesDirectQueryPort {

    List<Integer> findCourInfIdsByStatuses(List<Integer> statuses);
}

