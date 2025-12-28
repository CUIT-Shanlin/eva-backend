package edu.cuit.bc.evaluation.application.port;

import edu.cuit.domain.entity.eva.EvaTaskEntity;

import java.util.List;

/**
 * 评教任务“本人任务列表”等聚合查询端口（读侧）。
 *
 * <p>仅用于接口细分与依赖收敛，不改任何业务语义。</p>
 */
public interface EvaTaskSelfQueryPort {
    List<EvaTaskEntity> evaSelfTaskInfo(Integer useId, Integer id, String keyword);
}

