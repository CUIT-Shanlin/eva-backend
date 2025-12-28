package edu.cuit.bc.evaluation.application.port;

import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.EvaLogConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.eva.EvaRecordEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 评教记录读侧查询端口。
 */
public interface EvaRecordQueryPort extends EvaRecordScoreQueryPort, EvaRecordPagingQueryPort, EvaRecordUserLogQueryPort, EvaRecordCourseQueryPort {
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
