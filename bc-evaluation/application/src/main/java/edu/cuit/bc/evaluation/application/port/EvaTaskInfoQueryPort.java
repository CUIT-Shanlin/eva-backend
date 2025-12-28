package edu.cuit.bc.evaluation.application.port;

import edu.cuit.domain.entity.eva.EvaTaskEntity;

import java.util.Optional;

/**
 * 评教任务“单任务信息/任务名称”等聚合查询端口（读侧）。
 *
 * <p>仅用于接口细分与依赖收敛，不改任何业务语义。</p>
 */
public interface EvaTaskInfoQueryPort {
    Optional<EvaTaskEntity> oneEvaTaskInfo(Integer id);

    Optional<String> getNameByTaskId(Integer taskId);
}
