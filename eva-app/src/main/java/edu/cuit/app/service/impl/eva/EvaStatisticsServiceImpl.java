package edu.cuit.app.service.impl.eva;
import cn.hutool.json.JSONUtil;
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
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class EvaStatisticsServiceImpl implements IEvaStatisticsService {
    private final EvaQueryGateway evaQueryGateway;
    private final PaginationBizConvertor paginationBizConvertor;
    @Override
    @CheckSemId
    public EvaScoreInfoCO evaScoreStatisticsInfo(Integer semId, Number score) {
        return evaQueryGateway.evaScoreStatisticsInfo(semId,score).orElseThrow(()->new SysException("没有相关数据"));
    }

    @Override
    @CheckSemId
    public EvaSituationCO evaTemplateSituation(Integer semId) {
        return evaQueryGateway.evaTemplateSituation(semId).orElseThrow(()->new SysException("没有相关数据"));
    }

    @Override
    @CheckSemId
    public OneDayAddEvaDataCO evaOneDayInfo(Integer day, Integer num, Integer semId) {
        return evaQueryGateway.evaOneDayInfo(day,num,semId).orElseThrow(()->new SysException("未找到相关数据"));
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
        return evaQueryGateway.getEvaData(semId,num,target,evaTarget).orElseThrow(()->new SysException("没有找到相关数据"));
    }

    @Override
    @CheckSemId
    public PaginationQueryResultCO<UnqualifiedUserInfoCO> pageUnqualifiedUser(Integer semId,Integer type, Integer target, PagingQuery<UnqualifiedUserConditionalQuery> query) {
        if(type==0){
            PaginationResultEntity<UnqualifiedUserInfoCO> page=evaQueryGateway.pageEvaUnqualifiedUserInfo(semId,query,target);
            return paginationBizConvertor.toPaginationEntity(page,page.getRecords());
        } else if(type==1){
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
        UnqualifiedUserResultCO error=new UnqualifiedUserResultCO();
        error.setTotal(0).setDataArr(List.of());
        if(type==0){
            unqualifiedUserResultCO=evaQueryGateway.getEvaTargetAmountUnqualifiedUser(semId,num,target).orElseGet(()->error);
        } else if(type==1){
            unqualifiedUserResultCO=evaQueryGateway.getBeEvaTargetAmountUnqualifiedUser(semId,num,target).orElseGet(()->error);
        }else {
            throw new SysException("type是10以外的值");
        }
        return unqualifiedUserResultCO;
    }
}
