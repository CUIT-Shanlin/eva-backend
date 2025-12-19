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
import edu.cuit.infra.bcevaluation.query.EvaQueryRepo;
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
    private final EvaQueryRepo repo;

    @Override
    public PaginationResultEntity<EvaRecordEntity> pageEvaRecord(Integer semId, PagingQuery<EvaLogConditionalQuery> evaLogQuery) {
        return repo.pageEvaRecord(semId, evaLogQuery);
    }

    @Override
    public PaginationResultEntity<EvaTaskEntity> pageEvaUnfinishedTask(Integer semId, PagingQuery<EvaTaskConditionalQuery> taskQuery) {
        return repo.pageEvaUnfinishedTask(semId, taskQuery);
    }

    @Override
    public PaginationResultEntity<EvaTemplateEntity> pageEvaTemplate(Integer semId, PagingQuery<GenericConditionalQuery> query) {
        return repo.pageEvaTemplate(semId, query);
    }

    @Override
    public List<EvaTaskEntity> evaSelfTaskInfo(Integer useId, Integer id, String keyword) {
        return repo.evaSelfTaskInfo(useId, id, keyword);
    }

    @Override
    public List<EvaRecordEntity> getEvaLogInfo(Integer userId, Integer id, String keyword) {
        return repo.getEvaLogInfo(userId, id, keyword);
    }

    @Override
    public List<EvaRecordEntity> getEvaEdLogInfo(Integer userId, Integer semId, Integer courseId) {
        return repo.getEvaEdLogInfo(userId, semId, courseId);
    }

    @Override
    public Optional<EvaTaskEntity> oneEvaTaskInfo(Integer id) {
        return repo.oneEvaTaskInfo(id);
    }

    @Override
    public Optional<EvaScoreInfoCO> evaScoreStatisticsInfo(Integer semId, Number score) {
        return repo.evaScoreStatisticsInfo(semId, score);
    }

    @Override
    public Optional<EvaSituationCO> evaTemplateSituation(Integer semId) {
        return repo.evaTemplateSituation(semId);
    }

    @Override
    public List<Integer> getMonthEvaNUmber(Integer semId) {
        return repo.getMonthEvaNUmber(semId);
    }

    @Override
    public Optional<EvaWeekAddCO> evaWeekAdd(Integer week, Integer semId) {
        return repo.evaWeekAdd(week, semId);
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
    public PaginationResultEntity<UnqualifiedUserInfoCO> pageEvaUnqualifiedUserInfo(Integer semId, PagingQuery<UnqualifiedUserConditionalQuery> query, Integer target) {
        return repo.pageEvaUnqualifiedUserInfo(semId, query, target);
    }

    @Override
    public PaginationResultEntity<UnqualifiedUserInfoCO> pageBeEvaUnqualifiedUserInfo(Integer semId, PagingQuery<UnqualifiedUserConditionalQuery> query, Integer target) {
        return repo.pageBeEvaUnqualifiedUserInfo(semId, query, target);
    }

    @Override
    public Optional<Integer> getEvaNumber(Long id) {
        return repo.getEvaNumber(id);
    }

    @Override
    public Optional<String> getTaskTemplate(Integer taskId, Integer semId) {
        return repo.getTaskTemplate(taskId, semId);
    }

    @Override
    public List<ScoreRangeCourseCO> scoreRangeCourseInfo(Integer num, Integer interval) {
        return repo.scoreRangeCourseInfo(num, interval);
    }

    @Override
    public List<EvaTemplateEntity> getAllTemplate() {
        return repo.getAllTemplate();
    }

    @Override
    public Optional<Double> getScoreFromRecord(String prop) {
        return repo.getScoreFromRecord(prop);
    }

    @Override
    public Optional<Integer> getEvaNumByCourInfo(Integer courInfId) {
        return repo.getEvaNumByCourInfo(courInfId);
    }

    @Override
    public Optional<Integer> getEvaNumByCourse(Integer courseId) {
        return repo.getEvaNumByCourse(courseId);
    }

    @Override
    public Optional<String> getNameByTaskId(Integer taskId) {
        return repo.getNameByTaskId(taskId);
    }

    @Override
    public List<EvaRecordEntity> getRecordByCourse(Integer courseId) {
        return repo.getRecordByCourse(courseId);
    }

    @Override
    public Optional<Double> getScoreByProp(String prop) {
        return repo.getScoreByProp(prop);
    }

    @Override
    public List<Double> getScoresByProp(String props) {
        return repo.getScoresByProp(props);
    }

    @Override
    public Map<String, Double> getScorePropMapByProp(String props) {
        return repo.getScorePropMapByProp(props);
    }

    @Override
    public List<Integer> getCountAbEva(Integer semId, Integer userId) {
        return repo.getCountAbEva(semId, userId);
    }
}
