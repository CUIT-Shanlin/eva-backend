package edu.cuit.bc.evaluation.application.port;

import edu.cuit.client.dto.clientobject.eva.EvaScoreInfoCO;
import edu.cuit.client.dto.clientobject.eva.EvaSituationCO;
import edu.cuit.client.dto.clientobject.eva.EvaWeekAddCO;
import edu.cuit.client.dto.clientobject.eva.PastTimeEvaDetailCO;
import edu.cuit.client.dto.clientobject.eva.ScoreRangeCourseCO;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserInfoCO;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserResultCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.UnqualifiedUserConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;

import java.util.List;
import java.util.Optional;

/**
 * 评教统计读侧查询端口。
 *
 * <p>面向统计与导出场景的查询能力，逐步替代聚合式网关依赖。</p>
 */
public interface EvaStatisticsQueryPort {
    Optional<EvaScoreInfoCO> evaScoreStatisticsInfo(Integer semId, Number score);

    Optional<EvaSituationCO> evaTemplateSituation(Integer semId);

    Optional<EvaWeekAddCO> evaWeekAdd(Integer week, Integer semId);

    List<ScoreRangeCourseCO> scoreRangeCourseInfo(Integer num, Integer interval);

    List<Integer> getMonthEvaNUmber(Integer semId);

    Optional<PastTimeEvaDetailCO> getEvaData(Integer semId, Integer num, Integer target, Integer evaTarget);

    Optional<UnqualifiedUserResultCO> getEvaTargetAmountUnqualifiedUser(Integer semId, Integer num, Integer target);

    Optional<UnqualifiedUserResultCO> getBeEvaTargetAmountUnqualifiedUser(Integer semId, Integer num, Integer target);

    PaginationResultEntity<UnqualifiedUserInfoCO> pageEvaUnqualifiedUserInfo(Integer semId,
                                                                             PagingQuery<UnqualifiedUserConditionalQuery> query,
                                                                             Integer target);

    PaginationResultEntity<UnqualifiedUserInfoCO> pageBeEvaUnqualifiedUserInfo(Integer semId,
                                                                               PagingQuery<UnqualifiedUserConditionalQuery> query,
                                                                               Integer target);

    List<Integer> getCountAbEva(Integer semId, Integer userId);
}
