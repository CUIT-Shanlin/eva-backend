package edu.cuit.bc.evaluation.application.port;

import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.EvaTaskConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.eva.EvaTaskEntity;

import java.util.List;
import java.util.Optional;

/**
 * 评教任务读侧查询端口。
 */
public interface EvaTaskQueryPort extends EvaTaskInfoQueryPort {
    PaginationResultEntity<EvaTaskEntity> pageEvaUnfinishedTask(Integer semId, PagingQuery<EvaTaskConditionalQuery> taskQuery);

    List<EvaTaskEntity> evaSelfTaskInfo(Integer useId, Integer id, String keyword);

    Optional<EvaTaskEntity> oneEvaTaskInfo(Integer id);

    Optional<Integer> getEvaNumber(Long id);

    Optional<String> getNameByTaskId(Integer taskId);
}
