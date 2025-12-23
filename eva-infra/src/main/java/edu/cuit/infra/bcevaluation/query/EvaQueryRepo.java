package edu.cuit.infra.bcevaluation.query;

import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.EvaTaskConditionalQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.eva.EvaTaskEntity;
import edu.cuit.domain.entity.eva.EvaTemplateEntity;

import java.util.List;
import java.util.Optional;

/**
 * 评教读侧 QueryRepo（将复杂查询与组装从 gateway 中抽离）。
 *
 * <p>注意：此处是“渐进式结构化”的第一步，暂不引入 CQRS 投影表，保持行为不变。</p>
 */
public interface EvaQueryRepo extends EvaStatisticsQueryRepo, EvaRecordQueryRepo {
    PaginationResultEntity<EvaTaskEntity> pageEvaUnfinishedTask(Integer semId, PagingQuery<EvaTaskConditionalQuery> taskQuery);

    PaginationResultEntity<EvaTemplateEntity> pageEvaTemplate(Integer semId, PagingQuery<GenericConditionalQuery> query);

    List<EvaTaskEntity> evaSelfTaskInfo(Integer useId, Integer id, String keyword);

    Optional<EvaTaskEntity> oneEvaTaskInfo(Integer id);

    Optional<Integer> getEvaNumber(Long id);

    Optional<String> getTaskTemplate(Integer taskId, Integer semId);

    List<EvaTemplateEntity> getAllTemplate();

    Optional<String> getNameByTaskId(Integer taskId);
}
