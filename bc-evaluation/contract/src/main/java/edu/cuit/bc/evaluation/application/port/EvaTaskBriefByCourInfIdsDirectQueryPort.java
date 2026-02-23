package edu.cuit.bc.evaluation.application.port;

import edu.cuit.client.dto.clientobject.eva.EvaTaskBriefCO;
import java.util.List;

/**
 * 评教任务“最小视图”直查端口（读侧）。
 *
 * <p>用于跨 BC 按 courInfIds 查询评教任务列表；实现侧需保持与既有 SQL 的查询条件、结果顺序与空值语义一致，
 * 且不引入缓存/切面等副作用。</p>
 */
public interface EvaTaskBriefByCourInfIdsDirectQueryPort {

    List<EvaTaskBriefCO> findTaskBriefListByCourInfIds(List<Integer> courInfIds);
}

