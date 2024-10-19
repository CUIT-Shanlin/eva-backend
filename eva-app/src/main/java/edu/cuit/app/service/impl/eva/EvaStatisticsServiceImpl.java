package edu.cuit.app.service.impl.eva;
import com.alibaba.cola.exception.SysException;
import edu.cuit.app.aop.CheckSemId;
import edu.cuit.app.convertor.PaginationBizConvertor;
import edu.cuit.client.api.eva.IEvaStatisticsService;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.eva.*;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserInfoCO;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserResultCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.UnqualifiedUserConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.gateway.eva.EvaQueryGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaStatisticsServiceImpl implements IEvaStatisticsService {
    private final EvaQueryGateway evaQueryGateway;
    private final PaginationBizConvertor paginationBizConvertor;
    @Override
    @CheckSemId
    public EvaScoreInfoCO evaScoreStatisticsInfo(Integer semId, Number score) {
        return evaQueryGateway.evaScoreStatisticsInfo(semId,score).get();
    }

    @Override
    @CheckSemId
    public EvaSituationCO evaTemplateSituation(Integer semId) {
        return evaQueryGateway.evaTemplateSituation(semId).get();
    }

    @Override
    @CheckSemId
    public OneDayAddEvaDataCO evaOneDayInfo(Integer day, Integer num, Integer semId) {
        return evaQueryGateway.evaOneDayInfo(day,num,semId).get();
    }

    @Override
    public List<ScoreRangeCourseCO> scoreRangeCourseInfo(Integer num, Integer interval) {
        return evaQueryGateway.scoreRangeCourseInfo(num,interval);
    }

    @Override
    @CheckSemId
    public List<Integer> getMonthEvaNUmber(Integer semId) {
        return evaQueryGateway.getMonthEvaNUmber(semId);
    }

    @Override
    @CheckSemId
    public PastTimeEvaDetailCO getEvaData(Integer semId, Integer num, Integer target, Integer evaTarget) {
        return evaQueryGateway.getEvaData(semId,num,target,evaTarget).get();
    }

    @Override
    @CheckSemId
    public PaginationQueryResultCO<UnqualifiedUserInfoCO> pageUnqualifiedUser(Integer semId,Integer type, Integer target, PagingQuery<UnqualifiedUserConditionalQuery> query) {
        if(type==1){
            PaginationResultEntity<UnqualifiedUserInfoCO> page=evaQueryGateway.pageEvaUnqualifiedUserInfo(semId,query,target);
            return paginationBizConvertor.toPaginationEntity(page,page.getRecords());
        } else if(type==0){
            PaginationResultEntity<UnqualifiedUserInfoCO> page=evaQueryGateway.pageBeEvaUnqualifiedUserInfo(semId,query,target);
            return paginationBizConvertor.toPaginationEntity(page,page.getRecords());
        }else {
            throw new SysException("type是10以外的值");
        }

    }

    @Override
    @CheckSemId
    public UnqualifiedUserResultCO getTargetAmountUnqualifiedUser(Integer semId,Integer type, Integer num, Integer target) {

        UnqualifiedUserResultCO unqualifiedUserResultCO=null;
        if(type==1){
            unqualifiedUserResultCO=evaQueryGateway.getEvaTargetAmountUnqualifiedUser(semId,null,num,target).get();
        } else if(type==0){
            unqualifiedUserResultCO=evaQueryGateway.getBeEvaTargetAmountUnqualifiedUser(semId,null,num,target).get();
        }else {
            throw new SysException("type是10以外的值");
        }
        return unqualifiedUserResultCO;
    }
}
