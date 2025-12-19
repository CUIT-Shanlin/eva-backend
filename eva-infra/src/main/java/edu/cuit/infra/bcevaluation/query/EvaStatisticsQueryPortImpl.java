package edu.cuit.infra.bcevaluation.query;

import edu.cuit.bc.evaluation.application.port.EvaStatisticsQueryPort;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 评教统计读侧查询端口实现（委托 QueryRepo，保持行为不变）。
 */
@Component
@RequiredArgsConstructor
public class EvaStatisticsQueryPortImpl implements EvaStatisticsQueryPort {
    private final EvaQueryRepo repo;

    @Override
    public Optional<EvaScoreInfoCO> evaScoreStatisticsInfo(Integer semId, Number score) {
        return repo.evaScoreStatisticsInfo(semId, score);
    }

    @Override
    public Optional<EvaSituationCO> evaTemplateSituation(Integer semId) {
        return repo.evaTemplateSituation(semId);
    }

    @Override
    public Optional<EvaWeekAddCO> evaWeekAdd(Integer week, Integer semId) {
        return repo.evaWeekAdd(week, semId);
    }

    @Override
    public List<ScoreRangeCourseCO> scoreRangeCourseInfo(Integer num, Integer interval) {
        return repo.scoreRangeCourseInfo(num, interval);
    }

    @Override
    public List<Integer> getMonthEvaNUmber(Integer semId) {
        return repo.getMonthEvaNUmber(semId);
    }

    @Override
    public Optional<PastTimeEvaDetailCO> getEvaData(Integer semId, Integer num, Integer target, Integer evaTarget) {
        return repo.getEvaData(semId, num, target, evaTarget);
    }

    @Override
    public Optional<UnqualifiedUserResultCO> getEvaTargetAmountUnqualifiedUser(Integer semId, Integer num, Integer target) {
        return repo.getEvaTargetAmountUnqualifiedUser(semId, num, target);
    }

    @Override
    public Optional<UnqualifiedUserResultCO> getBeEvaTargetAmountUnqualifiedUser(Integer semId, Integer num, Integer target) {
        return repo.getBeEvaTargetAmountUnqualifiedUser(semId, num, target);
    }

    @Override
    public PaginationResultEntity<UnqualifiedUserInfoCO> pageEvaUnqualifiedUserInfo(Integer semId,
                                                                                    PagingQuery<UnqualifiedUserConditionalQuery> query,
                                                                                    Integer target) {
        return repo.pageEvaUnqualifiedUserInfo(semId, query, target);
    }

    @Override
    public PaginationResultEntity<UnqualifiedUserInfoCO> pageBeEvaUnqualifiedUserInfo(Integer semId,
                                                                                      PagingQuery<UnqualifiedUserConditionalQuery> query,
                                                                                      Integer target) {
        return repo.pageBeEvaUnqualifiedUserInfo(semId, query, target);
    }

    @Override
    public List<Integer> getCountAbEva(Integer semId, Integer userId) {
        return repo.getCountAbEva(semId, userId);
    }
}
