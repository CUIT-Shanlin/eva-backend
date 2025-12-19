package edu.cuit.bc.evaluation.application.port;

import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.eva.EvaTemplateEntity;

import java.util.List;
import java.util.Optional;

/**
 * 评教模板读侧查询端口。
 */
public interface EvaTemplateQueryPort {
    PaginationResultEntity<EvaTemplateEntity> pageEvaTemplate(Integer semId, PagingQuery<GenericConditionalQuery> query);

    List<EvaTemplateEntity> getAllTemplate();

    Optional<String> getTaskTemplate(Integer taskId, Integer semId);
}
