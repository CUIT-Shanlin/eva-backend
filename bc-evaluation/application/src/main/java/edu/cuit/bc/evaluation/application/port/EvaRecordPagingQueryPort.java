package edu.cuit.bc.evaluation.application.port;

import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.EvaLogConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.eva.EvaRecordEntity;

/**
 * 评教记录“分页查询”等聚合查询端口（读侧）。
 *
 * <p>仅用于接口细分与依赖收敛，不改任何业务语义。</p>
 */
public interface EvaRecordPagingQueryPort {
    PaginationResultEntity<EvaRecordEntity> pageEvaRecord(Integer semId, PagingQuery<EvaLogConditionalQuery> evaLogQuery);
}
