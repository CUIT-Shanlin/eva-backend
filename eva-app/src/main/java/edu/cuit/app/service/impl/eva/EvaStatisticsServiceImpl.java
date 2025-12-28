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
import edu.cuit.domain.entity.eva.EvaConfigEntity;
import edu.cuit.domain.gateway.eva.EvaConfigGateway;
import edu.cuit.bc.evaluation.application.usecase.EvaStatisticsQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaStatisticsServiceImpl implements IEvaStatisticsService {
    private final EvaStatisticsQueryUseCase evaStatisticsQueryUseCase;
    private final PaginationBizConvertor paginationBizConvertor;
    private final EvaConfigGateway evaConfigGateway;
    @Override
    @CheckSemId
    public EvaScoreInfoCO evaScoreStatisticsInfo(Integer semId, Number score) {
        return evaStatisticsQueryUseCase.evaScoreStatisticsInfo(semId,score).orElseGet(()->new EvaScoreInfoCO());
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
        Integer target= evaConfigGateway.getMinEvaNum();
        Integer evaTarget= evaConfigGateway.getMinBeEvaNum();
        return evaStatisticsQueryUseCase.getEvaData(semId,num,target,evaTarget).orElseGet(()->new PastTimeEvaDetailCO());
    }

    @Override
    @CheckSemId
    public PaginationQueryResultCO<UnqualifiedUserInfoCO> pageUnqualifiedUser(Integer semId,Integer type, PagingQuery<UnqualifiedUserConditionalQuery> query) {
        EvaConfigEntity evaConfig = evaConfigGateway.getEvaConfig();
        if(type==0){
            PaginationResultEntity<UnqualifiedUserInfoCO> page=evaStatisticsQueryUseCase.pageEvaUnqualifiedUserInfo(semId,query,evaConfig.getMinEvaNum());
            return paginationBizConvertor.toPaginationEntity(page,page.getRecords());
        } else if(type==1){
            PaginationResultEntity<UnqualifiedUserInfoCO> page=evaStatisticsQueryUseCase.pageBeEvaUnqualifiedUserInfo(semId,query,evaConfig.getMinBeEvaNum());
            return paginationBizConvertor.toPaginationEntity(page,page.getRecords());
        }else {
            throw new SysException("type是10以外的值");
        }

    }

    @Override
    @CheckSemId
    public UnqualifiedUserResultCO getTargetAmountUnqualifiedUser(Integer semId,Integer type, Integer num) {

        UnqualifiedUserResultCO unqualifiedUserResultCO=null;
        UnqualifiedUserResultCO error=new UnqualifiedUserResultCO();
        error.setTotal(0).setDataArr(List.of());
        EvaConfigEntity evaConfig = evaConfigGateway.getEvaConfig();
        if(type==0){
            unqualifiedUserResultCO=evaStatisticsQueryUseCase.getEvaTargetAmountUnqualifiedUser(semId,num,evaConfig.getMinEvaNum()).orElseGet(()->error);
        } else if(type==1){
            unqualifiedUserResultCO=evaStatisticsQueryUseCase.getBeEvaTargetAmountUnqualifiedUser(semId,num,evaConfig.getMinBeEvaNum()).orElseGet(()->error);
        }else {
            throw new SysException("type是10以外的值");
        }
        return unqualifiedUserResultCO;
    }

    @Override
    @CheckSemId
    public byte[] exportEvaStatistics(Integer semId) {
        return EvaStatisticsExcelFactory.createExcelData(semId);
    }
}
