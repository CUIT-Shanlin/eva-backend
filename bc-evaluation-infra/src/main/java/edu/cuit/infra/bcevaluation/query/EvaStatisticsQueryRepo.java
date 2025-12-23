package edu.cuit.infra.bcevaluation.query;

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
 * 评教统计读侧 QueryRepo（从 {@link EvaQueryRepo} 渐进式拆分出来）。
 *
 * <p>保持行为不变：仅做接口拆分与依赖收敛，不调整统计口径与异常文案。</p>
 */
public interface EvaStatisticsQueryRepo {
    Optional<EvaScoreInfoCO> evaScoreStatisticsInfo(Integer semId, Number score);

    Optional<EvaSituationCO> evaTemplateSituation(Integer semId);

    List<Integer> getMonthEvaNUmber(Integer semId);

    Optional<EvaWeekAddCO> evaWeekAdd(Integer week, Integer semId);

    Optional<PastTimeEvaDetailCO> getEvaData(Integer semId, Integer num, Integer target, Integer evaTarget);

    Optional<UnqualifiedUserResultCO> getEvaTargetAmountUnqualifiedUser(Integer semId, Integer num, Integer target);

    Optional<UnqualifiedUserResultCO> getBeEvaTargetAmountUnqualifiedUser(Integer semId, Integer num, Integer target);

    PaginationResultEntity<UnqualifiedUserInfoCO> pageEvaUnqualifiedUserInfo(Integer semId,
                                                                             PagingQuery<UnqualifiedUserConditionalQuery> query,
                                                                             Integer target);

    PaginationResultEntity<UnqualifiedUserInfoCO> pageBeEvaUnqualifiedUserInfo(Integer semId,
                                                                               PagingQuery<UnqualifiedUserConditionalQuery> query,
                                                                               Integer target);

    List<ScoreRangeCourseCO> scoreRangeCourseInfo(Integer num, Integer interval);

    List<Integer> getCountAbEva(Integer semId, Integer userId);
}
