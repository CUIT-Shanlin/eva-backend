package edu.cuit.infra.bcevaluation.query;

import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.eva.EvaTemplateEntity;

import java.util.List;
import java.util.Optional;

/**
 * 评教模板读侧 QueryRepo（从 {@link EvaQueryRepo} 渐进式拆分出来）。
 *
 * <p>保持行为不变：仅做接口拆分与依赖收敛，不调整查询口径与异常文案。</p>
 */
public interface EvaTemplateQueryRepo {
    PaginationResultEntity<EvaTemplateEntity> pageEvaTemplate(Integer semId, PagingQuery<GenericConditionalQuery> query);

    List<EvaTemplateEntity> getAllTemplate();

    Optional<String> getTaskTemplate(Integer taskId, Integer semId);
}
