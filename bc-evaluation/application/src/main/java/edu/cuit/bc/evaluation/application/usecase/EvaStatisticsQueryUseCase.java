package edu.cuit.bc.evaluation.application.usecase;

import com.alibaba.cola.exception.SysException;
import edu.cuit.bc.evaluation.application.port.EvaStatisticsOverviewQueryPort;
import edu.cuit.bc.evaluation.application.port.EvaStatisticsTrendQueryPort;
import edu.cuit.bc.evaluation.application.port.EvaStatisticsUnqualifiedUserQueryPort;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
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
import edu.cuit.domain.entity.eva.EvaConfigEntity;
import edu.cuit.domain.gateway.eva.EvaConfigGateway;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 评教统计读侧查询用例（QueryUseCase）。
 *
 * <p>当前阶段仅做“依赖归位 + 委托壳”，不改变任何业务语义；
 * 后续可以逐步把统计用例编排从 eva-app 归位到此处。</p>
 */
public class EvaStatisticsQueryUseCase {
    private final EvaStatisticsOverviewQueryPort overviewQueryPort;
    private final EvaStatisticsTrendQueryPort trendQueryPort;
    private final EvaStatisticsUnqualifiedUserQueryPort unqualifiedUserQueryPort;
    private final EvaConfigGateway evaConfigGateway;

    public EvaStatisticsQueryUseCase(
            EvaStatisticsOverviewQueryPort overviewQueryPort,
            EvaStatisticsTrendQueryPort trendQueryPort,
            EvaStatisticsUnqualifiedUserQueryPort unqualifiedUserQueryPort,
            EvaConfigGateway evaConfigGateway
    ) {
        this.overviewQueryPort = Objects.requireNonNull(overviewQueryPort, "overviewQueryPort");
        this.trendQueryPort = Objects.requireNonNull(trendQueryPort, "trendQueryPort");
        this.unqualifiedUserQueryPort = Objects.requireNonNull(unqualifiedUserQueryPort, "unqualifiedUserQueryPort");
        this.evaConfigGateway = Objects.requireNonNull(evaConfigGateway, "evaConfigGateway");
    }

    public Optional<EvaScoreInfoCO> evaScoreStatisticsInfo(Integer semId, Number score) {
        return overviewQueryPort.evaScoreStatisticsInfo(semId, score);
    }

    public EvaScoreInfoCO evaScoreStatisticsInfoOrEmpty(Integer semId, Number score) {
        return evaScoreStatisticsInfo(semId, score).orElseGet(() -> new EvaScoreInfoCO());
    }

    public Optional<EvaSituationCO> evaTemplateSituation(Integer semId) {
        return overviewQueryPort.evaTemplateSituation(semId);
    }

    public EvaSituationCO evaTemplateSituationOrEmpty(Integer semId) {
        return evaTemplateSituation(semId).orElseGet(() -> new EvaSituationCO());
    }

    public Optional<EvaWeekAddCO> evaWeekAdd(Integer week, Integer semId) {
        return trendQueryPort.evaWeekAdd(week, semId);
    }

    public EvaWeekAddCO evaWeekAddOrEmpty(Integer week, Integer semId) {
        return evaWeekAdd(week, semId).orElseGet(() -> new EvaWeekAddCO());
    }

    public List<ScoreRangeCourseCO> scoreRangeCourseInfo(Integer num, Integer interval) {
        return overviewQueryPort.scoreRangeCourseInfo(num, interval);
    }

    public List<Integer> getMonthEvaNUmber(Integer semId) {
        return trendQueryPort.getMonthEvaNUmber(semId);
    }

    public Optional<PastTimeEvaDetailCO> getEvaData(Integer semId, Integer num) {
        Integer target = evaConfigGateway.getMinEvaNum();
        Integer evaTarget = evaConfigGateway.getMinBeEvaNum();
        return getEvaData(semId, num, target, evaTarget);
    }

    public PastTimeEvaDetailCO getEvaDataOrEmpty(Integer semId, Integer num) {
        return getEvaData(semId, num).orElseGet(PastTimeEvaDetailCO::new);
    }

    public Optional<PastTimeEvaDetailCO> getEvaData(Integer semId, Integer num, Integer target, Integer evaTarget) {
        return overviewQueryPort.getEvaData(semId, num, target, evaTarget);
    }

    public Optional<UnqualifiedUserResultCO> getEvaTargetAmountUnqualifiedUser(Integer semId, Integer num, Integer target) {
        return unqualifiedUserQueryPort.getEvaTargetAmountUnqualifiedUser(semId, num, target);
    }

    public Optional<UnqualifiedUserResultCO> getBeEvaTargetAmountUnqualifiedUser(Integer semId, Integer num, Integer target) {
        return unqualifiedUserQueryPort.getBeEvaTargetAmountUnqualifiedUser(semId, num, target);
    }

    public UnqualifiedUserResultCO getTargetAmountUnqualifiedUser(
            Integer semId,
            Integer type,
            Integer num,
            EvaConfigEntity evaConfig,
            UnqualifiedUserResultCO error
    ) {
        if (type == 0) {
            return getEvaTargetAmountUnqualifiedUser(semId, num, evaConfig.getMinEvaNum())
                    .orElseGet(() -> error);
        } else if (type == 1) {
            return getBeEvaTargetAmountUnqualifiedUser(semId, num, evaConfig.getMinBeEvaNum())
                    .orElseGet(() -> error);
        } else {
            throw new SysException("type是10以外的值");
        }
    }

    public UnqualifiedUserResultCO getTargetAmountUnqualifiedUser(
            Integer semId,
            Integer type,
            Integer num,
            UnqualifiedUserResultCO error
    ) {
        EvaConfigEntity evaConfig = evaConfigGateway.getEvaConfig();
        return getTargetAmountUnqualifiedUser(semId, type, num, evaConfig, error);
    }

    public UnqualifiedUserResultCO getTargetAmountUnqualifiedUserOrEmpty(Integer semId, Integer type, Integer num) {
        UnqualifiedUserResultCO error = new UnqualifiedUserResultCO();
        error.setTotal(0).setDataArr(List.of());
        return getTargetAmountUnqualifiedUser(semId, type, num, error);
    }

    public PaginationResultEntity<UnqualifiedUserInfoCO> pageEvaUnqualifiedUserInfo(
            Integer semId,
            PagingQuery<UnqualifiedUserConditionalQuery> query,
            Integer target
    ) {
        return unqualifiedUserQueryPort.pageEvaUnqualifiedUserInfo(semId, query, target);
    }

    public PaginationResultEntity<UnqualifiedUserInfoCO> pageBeEvaUnqualifiedUserInfo(
            Integer semId,
            PagingQuery<UnqualifiedUserConditionalQuery> query,
            Integer target
    ) {
        return unqualifiedUserQueryPort.pageBeEvaUnqualifiedUserInfo(semId, query, target);
    }

    public PaginationResultEntity<UnqualifiedUserInfoCO> pageUnqualifiedUser(
            Integer semId,
            Integer type,
            PagingQuery<UnqualifiedUserConditionalQuery> query,
            EvaConfigEntity evaConfig
    ) {
        if (type == 0) {
            return pageEvaUnqualifiedUserInfo(semId, query, evaConfig.getMinEvaNum());
        } else if (type == 1) {
            return pageBeEvaUnqualifiedUserInfo(semId, query, evaConfig.getMinBeEvaNum());
        } else {
            throw new SysException("type是10以外的值");
        }
    }

    public PaginationResultEntity<UnqualifiedUserInfoCO> pageUnqualifiedUser(
            Integer semId,
            Integer type,
            PagingQuery<UnqualifiedUserConditionalQuery> query
    ) {
        EvaConfigEntity evaConfig = evaConfigGateway.getEvaConfig();
        return pageUnqualifiedUser(semId, type, query, evaConfig);
    }

    public PaginationQueryResultCO<UnqualifiedUserInfoCO> pageUnqualifiedUserAsPaginationQueryResult(
            Integer semId,
            Integer type,
            PagingQuery<UnqualifiedUserConditionalQuery> query
    ) {
        PaginationResultEntity<UnqualifiedUserInfoCO> page = pageUnqualifiedUser(semId, type, query);
        PaginationQueryResultCO<UnqualifiedUserInfoCO> result = new PaginationQueryResultCO<>();
        result.setCurrent(page.getCurrent())
                .setSize(page.getSize())
                .setTotal(page.getTotal())
                .setRecords(page.getRecords());
        return result;
    }

    public List<Integer> getCountAbEva(Integer semId, Integer userId) {
        return overviewQueryPort.getCountAbEva(semId, userId);
    }
}
