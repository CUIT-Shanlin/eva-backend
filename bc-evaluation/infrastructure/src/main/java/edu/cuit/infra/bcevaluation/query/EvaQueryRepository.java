package edu.cuit.infra.bcevaluation.query;

import edu.cuit.client.dto.clientobject.eva.EvaScoreInfoCO;
import edu.cuit.client.dto.clientobject.eva.EvaSituationCO;
import edu.cuit.client.dto.clientobject.eva.EvaWeekAddCO;
import edu.cuit.client.dto.clientobject.eva.PastTimeEvaDetailCO;
import edu.cuit.client.dto.clientobject.eva.ScoreRangeCourseCO;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 评教查询仓储（聚合门面）：对外保持 {@link EvaQueryRepo} 不变；内部按“统计/任务/记录/模板”四主题仓储委托。
 * <p>
 * 说明：此类仅做委托壳，避免继续承载历史私有组装逻辑（行为保持不变）。
 */
@Component
@RequiredArgsConstructor
public class EvaQueryRepository implements EvaQueryRepo {
    private final EvaStatisticsQueryRepository evaStatisticsQueryRepository;
    private final EvaRecordQueryRepository evaRecordQueryRepository;
    private final EvaTaskQueryRepository evaTaskQueryRepository;
    private final EvaTemplateQueryRepository evaTemplateQueryRepository;

    @Override
    public PaginationResultEntity<EvaRecordEntity> pageEvaRecord(Integer semId, PagingQuery<EvaLogConditionalQuery> query) {
        return evaRecordQueryRepository.pageEvaRecord(semId, query);
    }

    @Override
    public PaginationResultEntity<EvaTaskEntity> pageEvaUnfinishedTask(Integer semId, PagingQuery<EvaTaskConditionalQuery> taskQuery) {
        return evaTaskQueryRepository.pageEvaUnfinishedTask(semId, taskQuery);
    }

    @Override
    public PaginationResultEntity<EvaTemplateEntity> pageEvaTemplate(Integer semId, PagingQuery<GenericConditionalQuery> query) {
        return evaTemplateQueryRepository.pageEvaTemplate(semId, query);
    }


    //zjok
    @Override
    public List<EvaTaskEntity> evaSelfTaskInfo(Integer userId, Integer id, String keyword) {
        return evaTaskQueryRepository.evaSelfTaskInfo(userId, id, keyword);
    }
    //zjok
    @Override
    public List<EvaRecordEntity> getEvaLogInfo(Integer evaUserId, Integer id, String keyword) {
        return evaRecordQueryRepository.getEvaLogInfo(evaUserId, id, keyword);
    }
    //zjok
    @Override
    public List<EvaRecordEntity> getEvaEdLogInfo(Integer userId, Integer semId, Integer courseId) {
        return evaRecordQueryRepository.getEvaEdLogInfo(userId, semId, courseId);
    }

    //zjok
    @Override
    public Optional<EvaTaskEntity> oneEvaTaskInfo(Integer id) {
        return evaTaskQueryRepository.oneEvaTaskInfo(id);
    }
    //zjok
    @Override
    public Optional<EvaScoreInfoCO> evaScoreStatisticsInfo(Integer semId, Number score) {
        return evaStatisticsQueryRepository.evaScoreStatisticsInfo(semId, score);
    }
    //zjok
    @Override
    public Optional<EvaSituationCO> evaTemplateSituation(Integer semId) {
        return evaStatisticsQueryRepository.evaTemplateSituation(semId);
    }

    //zjok
    @Override
    public List<Integer> getMonthEvaNUmber(Integer semId) {
        return evaStatisticsQueryRepository.getMonthEvaNUmber(semId);
    }
    //zjok
    @Override
    public Optional<EvaWeekAddCO> evaWeekAdd(Integer week, Integer semId) {
        return evaStatisticsQueryRepository.evaWeekAdd(week, semId);
    }

    //zjok
    @Override
    public Optional<PastTimeEvaDetailCO> getEvaData(Integer semId, Integer num, Integer target, Integer evaTarget) {
        return evaStatisticsQueryRepository.getEvaData(semId, num, target, evaTarget);
    }
    @Override
    public Optional<UnqualifiedUserResultCO> getEvaTargetAmountUnqualifiedUser(Integer semId, Integer num, Integer target) {
        return evaStatisticsQueryRepository.getEvaTargetAmountUnqualifiedUser(semId, num, target);
    }
    @Override
    public Optional<UnqualifiedUserResultCO> getBeEvaTargetAmountUnqualifiedUser(Integer semId, Integer num, Integer target) {
        return evaStatisticsQueryRepository.getBeEvaTargetAmountUnqualifiedUser(semId, num, target);
    }
    @Override
    public PaginationResultEntity<UnqualifiedUserInfoCO> pageEvaUnqualifiedUserInfo(Integer semId, PagingQuery<UnqualifiedUserConditionalQuery> query, Integer target) {
        return evaStatisticsQueryRepository.pageEvaUnqualifiedUserInfo(semId, query, target);
    }
    @Override
    public PaginationResultEntity<UnqualifiedUserInfoCO> pageBeEvaUnqualifiedUserInfo(Integer semId, PagingQuery<UnqualifiedUserConditionalQuery> query, Integer target) {
        return evaStatisticsQueryRepository.pageBeEvaUnqualifiedUserInfo(semId, query, target);
    }

    @Override
    public Optional<Integer> getEvaNumber(Long id) {
        return evaTaskQueryRepository.getEvaNumber(id);
    }

    //zjok
    @Override
    public Optional<String> getTaskTemplate(Integer taskId, Integer semId) {
        return evaTemplateQueryRepository.getTaskTemplate(taskId, semId);
    }

    @Override
    public List<ScoreRangeCourseCO> scoreRangeCourseInfo(Integer num, Integer interval) {
        return evaStatisticsQueryRepository.scoreRangeCourseInfo(num, interval);
    }

    @Override
    public List<EvaTemplateEntity> getAllTemplate() {
        return evaTemplateQueryRepository.getAllTemplate();
    }

    @Override
    public Optional<Double> getScoreFromRecord(String prop) {
        return evaRecordQueryRepository.getScoreFromRecord(prop);
    }

    @Override
    public Optional<Integer> getEvaNumByCourInfo(Integer courInfId) {
        return evaRecordQueryRepository.getEvaNumByCourInfo(courInfId);
    }

    @Override
    public Optional<Integer> getEvaNumByCourse(Integer courseId) {
        return evaRecordQueryRepository.getEvaNumByCourse(courseId);
    }

    @Override
    public Optional<String> getNameByTaskId(Integer taskId) {
        return evaTaskQueryRepository.getNameByTaskId(taskId);
    }

    @Override
    public List<EvaRecordEntity> getRecordByCourse(Integer courseId) {
        return evaRecordQueryRepository.getRecordByCourse(courseId);
    }

    @Override
    public Optional<Double> getScoreByProp(String prop) {
        return evaRecordQueryRepository.getScoreByProp(prop);
    }

    @Override
    public List<Double> getScoresByProp(String props) {
        return evaRecordQueryRepository.getScoresByProp(props);
    }

    @Override
    public Map<String, Double> getScorePropMapByProp(String props) {
        return evaRecordQueryRepository.getScorePropMapByProp(props);
    }

    @Override
    public List<Integer> getCountAbEva(Integer semId, Integer userId) {
        return evaStatisticsQueryRepository.getCountAbEva(semId, userId);
    }
}
