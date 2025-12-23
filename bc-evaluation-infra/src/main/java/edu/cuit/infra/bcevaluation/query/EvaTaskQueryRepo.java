package edu.cuit.infra.bcevaluation.query;

import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.EvaTaskConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.eva.EvaTaskEntity;

import java.util.List;
import java.util.Optional;

/**
 * 评教任务读侧 QueryRepo（从 {@link EvaQueryRepo} 渐进式拆分出来）。
 *
 * <p>保持行为不变：仅做接口拆分与依赖收敛，不调整查询口径与异常文案。</p>
 */
public interface EvaTaskQueryRepo {
    PaginationResultEntity<EvaTaskEntity> pageEvaUnfinishedTask(Integer semId, PagingQuery<EvaTaskConditionalQuery> taskQuery);

    List<EvaTaskEntity> evaSelfTaskInfo(Integer useId, Integer id, String keyword);

    Optional<EvaTaskEntity> oneEvaTaskInfo(Integer id);

    Optional<Integer> getEvaNumber(Long id);

    Optional<String> getNameByTaskId(Integer taskId);
}
