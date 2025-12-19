package edu.cuit.infra.gateway.impl.eva;

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
import edu.cuit.domain.gateway.eva.EvaQueryGateway;
import edu.cuit.bc.evaluation.application.port.EvaRecordQueryPort;
import edu.cuit.bc.evaluation.application.port.EvaStatisticsQueryPort;
import edu.cuit.bc.evaluation.application.port.EvaTaskQueryPort;
import edu.cuit.bc.evaluation.application.port.EvaTemplateQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 评教查询 gateway（读侧入口）。
 *
 * <p>说明：复杂查询与组装逻辑已抽取到 {@link EvaQueryRepo}，本类退化为委托壳，便于后续进一步模块化与 CQRS 演进。</p>
 */
@Component
@RequiredArgsConstructor
public class EvaQueryGatewayImpl implements EvaQueryGateway {
    private final EvaTaskQueryPort evaTaskQueryPort;
    private final EvaRecordQueryPort evaRecordQueryPort;
    private final EvaTemplateQueryPort evaTemplateQueryPort;
    private final EvaStatisticsQueryPort evaStatisticsQueryPort;

    @Override
    public PaginationResultEntity<EvaRecordEntity> pageEvaRecord(Integer semId, PagingQuery<EvaLogConditionalQuery> evaLogQuery) {
        return evaRecordQueryPort.pageEvaRecord(semId, evaLogQuery);
    }

    @Override
    public PaginationResultEntity<EvaTaskEntity> pageEvaUnfinishedTask(Integer semId, PagingQuery<EvaTaskConditionalQuery> taskQuery) {
        return evaTaskQueryPort.pageEvaUnfinishedTask(semId, taskQuery);
    }

    @Override
    public PaginationResultEntity<EvaTemplateEntity> pageEvaTemplate(Integer semId, PagingQuery<GenericConditionalQuery> query) {
        return evaTemplateQueryPort.pageEvaTemplate(semId, query);
    }

    @Override
    public List<EvaTaskEntity> evaSelfTaskInfo(Integer useId, Integer id, String keyword) {
        return evaTaskQueryPort.evaSelfTaskInfo(useId, id, keyword);
    }

    @Override
    public List<EvaRecordEntity> getEvaLogInfo(Integer userId, Integer id, String keyword) {
        return evaRecordQueryPort.getEvaLogInfo(userId, id, keyword);
    }

    @Override
    public List<EvaRecordEntity> getEvaEdLogInfo(Integer userId, Integer semId, Integer courseId) {
        return evaRecordQueryPort.getEvaEdLogInfo(userId, semId, courseId);
    }

    @Override
    public Optional<EvaTaskEntity> oneEvaTaskInfo(Integer id) {
        return evaTaskQueryPort.oneEvaTaskInfo(id);
    }

    @Override
    public Optional<EvaScoreInfoCO> evaScoreStatisticsInfo(Integer semId, Number score) {
        return evaStatisticsQueryPort.evaScoreStatisticsInfo(semId, score);
    }

    @Override
    public Optional<EvaSituationCO> evaTemplateSituation(Integer semId) {
        return evaStatisticsQueryPort.evaTemplateSituation(semId);
    }

    @Override
    public List<Integer> getMonthEvaNUmber(Integer semId) {
        return evaStatisticsQueryPort.getMonthEvaNUmber(semId);
    }

    @Override
    public Optional<EvaWeekAddCO> evaWeekAdd(Integer week, Integer semId) {
        return evaStatisticsQueryPort.evaWeekAdd(week, semId);
    }

    @Override
    public Optional<PastTimeEvaDetailCO> getEvaData(Integer semId, Integer num, Integer target, Integer evaTarget) {
        return evaStatisticsQueryPort.getEvaData(semId, num, target, evaTarget);
    }

    @Override
    public Optional<UnqualifiedUserResultCO> getEvaTargetAmountUnqualifiedUser(Integer semId, Integer num, Integer target) {
        return evaStatisticsQueryPort.getEvaTargetAmountUnqualifiedUser(semId, num, target);
    }

    @Override
    public Optional<UnqualifiedUserResultCO> getBeEvaTargetAmountUnqualifiedUser(Integer semId, Integer num, Integer target) {
        return evaStatisticsQueryPort.getBeEvaTargetAmountUnqualifiedUser(semId, num, target);
    }

    @Override
    public PaginationResultEntity<UnqualifiedUserInfoCO> pageEvaUnqualifiedUserInfo(Integer semId, PagingQuery<UnqualifiedUserConditionalQuery> query, Integer target) {
        return evaStatisticsQueryPort.pageEvaUnqualifiedUserInfo(semId, query, target);
    }

    @Override
    public PaginationResultEntity<UnqualifiedUserInfoCO> pageBeEvaUnqualifiedUserInfo(Integer semId, PagingQuery<UnqualifiedUserConditionalQuery> query, Integer target) {
        return evaStatisticsQueryPort.pageBeEvaUnqualifiedUserInfo(semId, query, target);
    }

    @Override
    public Optional<Integer> getEvaNumber(Long id) {
        return evaTaskQueryPort.getEvaNumber(id);
    }

    @Override
    public Optional<String> getTaskTemplate(Integer taskId, Integer semId) {
        return evaTemplateQueryPort.getTaskTemplate(taskId, semId);
    }

    @Override
    public List<ScoreRangeCourseCO> scoreRangeCourseInfo(Integer num, Integer interval) {
        return evaStatisticsQueryPort.scoreRangeCourseInfo(num, interval);
    }

    @Override
    public List<EvaTemplateEntity> getAllTemplate() {
        return evaTemplateQueryPort.getAllTemplate();
    }

    @Override
    public Optional<Double> getScoreFromRecord(String prop) {
        return evaRecordQueryPort.getScoreFromRecord(prop);
    }

    @Override
    public Optional<Integer> getEvaNumByCourInfo(Integer courInfId) {
        return evaRecordQueryPort.getEvaNumByCourInfo(courInfId);
    }

    @Override
    public Optional<Integer> getEvaNumByCourse(Integer courseId) {
        return evaRecordQueryPort.getEvaNumByCourse(courseId);
    }

    @Override
    public Optional<String> getNameByTaskId(Integer taskId) {
        return evaTaskQueryPort.getNameByTaskId(taskId);
    }

    @Override
    public List<EvaRecordEntity> getRecordByCourse(Integer courseId) {
        return evaRecordQueryPort.getRecordByCourse(courseId);
    }

    @Override
    public Optional<Double> getScoreByProp(String prop) {
        return evaRecordQueryPort.getScoreByProp(prop);
    }

    @Override
    public List<Double> getScoresByProp(String props) {
        return evaRecordQueryPort.getScoresByProp(props);
    }

    @Override
    public Map<String, Double> getScorePropMapByProp(String props) {
        return evaRecordQueryPort.getScorePropMapByProp(props);
    }

    @Override
    public List<Integer> getCountAbEva(Integer semId, Integer userId) {
        return evaStatisticsQueryPort.getCountAbEva(semId, userId);
    }
}
