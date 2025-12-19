package edu.cuit.infra.bcevaluation.query;

import edu.cuit.client.dto.clientobject.eva.*;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserInfoCO;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserResultCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.EvaLogConditionalQuery;
import edu.cuit.client.dto.query.condition.EvaTaskConditionalQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.client.dto.query.condition.UnqualifiedUserConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.eva.EvaRecordEntity;
import edu.cuit.domain.entity.eva.EvaTaskEntity;
import edu.cuit.domain.entity.eva.EvaTemplateEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 评教读侧 QueryRepo（将复杂查询与组装从 gateway 中抽离）。
 *
 * <p>注意：此处是“渐进式结构化”的第一步，暂不引入 CQRS 投影表，保持行为不变。</p>
 */
public interface EvaQueryRepo {
    PaginationResultEntity<EvaRecordEntity> pageEvaRecord(Integer semId, PagingQuery<EvaLogConditionalQuery> evaLogQuery);

    PaginationResultEntity<EvaTaskEntity> pageEvaUnfinishedTask(Integer semId, PagingQuery<EvaTaskConditionalQuery> taskQuery);

    PaginationResultEntity<EvaTemplateEntity> pageEvaTemplate(Integer semId, PagingQuery<GenericConditionalQuery> query);

    List<EvaTaskEntity> evaSelfTaskInfo(Integer useId, Integer id, String keyword);

    List<EvaRecordEntity> getEvaLogInfo(Integer userId, Integer id, String keyword);

    List<EvaRecordEntity> getEvaEdLogInfo(Integer userId, Integer semId, Integer courseId);

    Optional<EvaTaskEntity> oneEvaTaskInfo(Integer id);

    Optional<EvaScoreInfoCO> evaScoreStatisticsInfo(Integer semId, Number score);

    Optional<EvaSituationCO> evaTemplateSituation(Integer semId);

    List<Integer> getMonthEvaNUmber(Integer semId);

    Optional<EvaWeekAddCO> evaWeekAdd(Integer week, Integer semId);

    Optional<PastTimeEvaDetailCO> getEvaData(Integer semId, Integer num, Integer target, Integer evaTarget);

    Optional<UnqualifiedUserResultCO> getEvaTargetAmountUnqualifiedUser(Integer semId, Integer num, Integer target);

    Optional<UnqualifiedUserResultCO> getBeEvaTargetAmountUnqualifiedUser(Integer semId, Integer num, Integer target);

    PaginationResultEntity<UnqualifiedUserInfoCO> pageEvaUnqualifiedUserInfo(Integer semId, PagingQuery<UnqualifiedUserConditionalQuery> query, Integer target);

    PaginationResultEntity<UnqualifiedUserInfoCO> pageBeEvaUnqualifiedUserInfo(Integer semId, PagingQuery<UnqualifiedUserConditionalQuery> query, Integer target);

    Optional<Integer> getEvaNumber(Long id);

    Optional<String> getTaskTemplate(Integer taskId, Integer semId);

    List<ScoreRangeCourseCO> scoreRangeCourseInfo(Integer num, Integer interval);

    List<EvaTemplateEntity> getAllTemplate();

    Optional<Double> getScoreFromRecord(String prop);

    Optional<Integer> getEvaNumByCourInfo(Integer courInfId);

    Optional<Integer> getEvaNumByCourse(Integer courseId);

    Optional<String> getNameByTaskId(Integer taskId);

    List<EvaRecordEntity> getRecordByCourse(Integer courseId);

    Optional<Double> getScoreByProp(String prop);

    List<Double> getScoresByProp(String props);

    Map<String, Double> getScorePropMapByProp(String props);

    List<Integer> getCountAbEva(Integer semId, Integer userId);
}
