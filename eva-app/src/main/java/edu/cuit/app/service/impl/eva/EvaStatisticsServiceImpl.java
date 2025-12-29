package edu.cuit.app.service.impl.eva;
import com.alibaba.cola.exception.SysException;
import edu.cuit.app.aop.CheckSemId;
import edu.cuit.app.convertor.PaginationBizConvertor;
import edu.cuit.app.poi.eva.EvaStatisticsExcelFactory;
import edu.cuit.client.api.eva.IEvaStatisticsService;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.eva.*;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserInfoCO;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserResultCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.UnqualifiedUserConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.bc.evaluation.application.usecase.EvaStatisticsQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaStatisticsServiceImpl implements IEvaStatisticsService {
    private final EvaStatisticsQueryUseCase evaStatisticsQueryUseCase;
    private final PaginationBizConvertor paginationBizConvertor;
    @Override
    @CheckSemId
    public EvaScoreInfoCO evaScoreStatisticsInfo(Integer semId, Number score) {
        return evaStatisticsQueryUseCase.evaScoreStatisticsInfoOrEmpty(semId, score);
    }

    @Override
    @CheckSemId
    public EvaSituationCO evaTemplateSituation(Integer semId) {
        return evaStatisticsQueryUseCase.evaTemplateSituation(semId).orElseGet(()->new EvaSituationCO());
    }

    @Override
    @CheckSemId
    public EvaWeekAddCO evaWeekAdd(Integer week, Integer semId) {
        return evaStatisticsQueryUseCase.evaWeekAdd(week,semId).orElseGet(()->new EvaWeekAddCO());
    }

    @Override
    public List<ScoreRangeCourseCO> scoreRangeCourseInfo(Integer num, Integer interval) {
        return evaStatisticsQueryUseCase.scoreRangeCourseInfo(num,interval);
    }

    @Override
    @CheckSemId
    public List<Integer> getMonthEvaNUmber(Integer semId) {
        return evaStatisticsQueryUseCase.getMonthEvaNUmber(semId);
    }

    @Override
    @CheckSemId
    public PastTimeEvaDetailCO getEvaData(Integer semId, Integer num) {
        return evaStatisticsQueryUseCase.getEvaData(semId, num).orElseGet(() -> new PastTimeEvaDetailCO());
    }

    @Override
    @CheckSemId
    public PaginationQueryResultCO<UnqualifiedUserInfoCO> pageUnqualifiedUser(Integer semId,Integer type, PagingQuery<UnqualifiedUserConditionalQuery> query) {
        PaginationResultEntity<UnqualifiedUserInfoCO> page = evaStatisticsQueryUseCase.pageUnqualifiedUser(semId, type, query);
        return paginationBizConvertor.toPaginationEntity(page, page.getRecords());
    }

    @Override
    @CheckSemId
    public UnqualifiedUserResultCO getTargetAmountUnqualifiedUser(Integer semId,Integer type, Integer num) {

        UnqualifiedUserResultCO unqualifiedUserResultCO=null;
        UnqualifiedUserResultCO error=new UnqualifiedUserResultCO();
        error.setTotal(0).setDataArr(List.of());
        unqualifiedUserResultCO = evaStatisticsQueryUseCase.getTargetAmountUnqualifiedUser(semId, type, num, error);
        return unqualifiedUserResultCO;
    }

    @Override
    @CheckSemId
    public byte[] exportEvaStatistics(Integer semId) {
        return EvaStatisticsExcelFactory.createExcelData(semId);
    }
}
