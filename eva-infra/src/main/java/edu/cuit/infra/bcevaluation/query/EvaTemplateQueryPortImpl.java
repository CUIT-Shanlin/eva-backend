package edu.cuit.infra.bcevaluation.query;

import edu.cuit.bc.evaluation.application.port.EvaTemplateQueryPort;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.eva.EvaTemplateEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 评教模板读侧查询端口实现（委托 QueryRepo，保持行为不变）。
 */
@Component
@RequiredArgsConstructor
public class EvaTemplateQueryPortImpl implements EvaTemplateQueryPort {
    private final EvaTemplateQueryRepo repo;

    @Override
    public PaginationResultEntity<EvaTemplateEntity> pageEvaTemplate(Integer semId, PagingQuery<GenericConditionalQuery> query) {
        return repo.pageEvaTemplate(semId, query);
    }

    @Override
    public List<EvaTemplateEntity> getAllTemplate() {
        return repo.getAllTemplate();
    }

    @Override
    public Optional<String> getTaskTemplate(Integer taskId, Integer semId) {
        return repo.getTaskTemplate(taskId, semId);
    }
}
