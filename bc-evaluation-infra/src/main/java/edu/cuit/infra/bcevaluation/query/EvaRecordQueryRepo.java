package edu.cuit.infra.bcevaluation.query;

import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.EvaLogConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.eva.EvaRecordEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 评教记录读侧 QueryRepo（从 {@link EvaQueryRepo} 渐进式拆分出来）。
 *
 * <p>保持行为不变：仅做接口拆分与依赖收敛，不调整查询口径与异常文案。</p>
 */
public interface EvaRecordQueryRepo {
    PaginationResultEntity<EvaRecordEntity> pageEvaRecord(Integer semId, PagingQuery<EvaLogConditionalQuery> evaLogQuery);

    List<EvaRecordEntity> getEvaLogInfo(Integer userId, Integer id, String keyword);

    List<EvaRecordEntity> getEvaEdLogInfo(Integer userId, Integer semId, Integer courseId);

    Optional<Double> getScoreFromRecord(String prop);

    List<EvaRecordEntity> getRecordByCourse(Integer courseId);

    Optional<Double> getScoreByProp(String prop);

    List<Double> getScoresByProp(String props);

    Map<String, Double> getScorePropMapByProp(String props);

    Optional<Integer> getEvaNumByCourInfo(Integer courInfId);

    Optional<Integer> getEvaNumByCourse(Integer courseId);
}
