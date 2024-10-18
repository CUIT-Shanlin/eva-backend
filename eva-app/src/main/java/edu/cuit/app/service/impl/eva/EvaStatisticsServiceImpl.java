package edu.cuit.app.service.impl.eva;
import com.alibaba.cola.exception.BizException;
import com.alibaba.cola.exception.SysException;
import edu.cuit.app.aop.CheckSemId;
import edu.cuit.app.convertor.PaginationBizConvertor;
import edu.cuit.app.convertor.user.MenuBizConvertor;
import edu.cuit.client.api.eva.IEvaStatisticsService;
import edu.cuit.client.api.user.IMenuService;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.eva.*;
import edu.cuit.client.dto.clientobject.user.GenericMenuSectionCO;
import edu.cuit.client.dto.clientobject.user.MenuCO;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserInfoCO;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserResultCO;
import edu.cuit.client.dto.cmd.user.NewMenuCmd;
import edu.cuit.client.dto.cmd.user.UpdateMenuCmd;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.MenuConditionalQuery;
import edu.cuit.client.dto.query.condition.UnqualifiedUserConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.gateway.eva.EvaDeleteGateway;
import edu.cuit.domain.gateway.eva.EvaQueryGateway;
import edu.cuit.domain.gateway.eva.EvaUpdateGateway;
import edu.cuit.domain.gateway.user.MenuQueryGateway;
import edu.cuit.domain.gateway.user.MenuUpdateGateway;
import edu.cuit.infra.convertor.PaginationConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaStatisticsServiceImpl implements IEvaStatisticsService {
    private final EvaDeleteGateway evaDeleteGateway;
    private final EvaUpdateGateway evaUpdateGateway;
    private final EvaQueryGateway evaQueryGateway;
    private final PaginationBizConvertor paginationBizConvertor;
    @Override
    @CheckSemId
    public EvaScoreInfoCO evaScoreStatisticsInfo(Integer semId, Number score) {
        EvaScoreInfoCO evaScoreInfoCO=evaQueryGateway.evaScoreStatisticsInfo(semId,score).get();
        return evaScoreInfoCO;
    }

    @Override
    @CheckSemId
    public EvaSituationCO evaTemplateSituation(Integer semId) {
        return null;
    }

    @Override
    @CheckSemId
    public OneDayAddEvaDataCO evaOneDayInfo(Integer day, Integer num, Integer semId) {
        return null;
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
    public PaginationQueryResultCO<UnqualifiedUserInfoCO> pageUnqualifiedUser(Integer type, Integer target, PagingQuery<UnqualifiedUserConditionalQuery> query) {
        if(type==1){
            PaginationResultEntity<UnqualifiedUserInfoCO> page=evaQueryGateway.pageEvaUnqualifiedUserInfo(query,target);
            return paginationBizConvertor.toPaginationEntity(page,page.getRecords());
        } else if(type==0){
            PaginationResultEntity<UnqualifiedUserInfoCO> page=evaQueryGateway.pageBeEvaUnqualifiedUserInfo(query,target);
            return paginationBizConvertor.toPaginationEntity(page,page.getRecords());
        }else {
            throw new SysException("type是10以外的值");
        }

    }

    @Override
    public UnqualifiedUserResultCO getTargetAmountUnqualifiedUser(Integer type, Integer num, Integer target) {

        UnqualifiedUserResultCO unqualifiedUserResultCO=null;
        if(type==1){
            unqualifiedUserResultCO=evaQueryGateway.getEvaTargetAmountUnqualifiedUser(null,num,target).get();
        } else if(type==0){
            unqualifiedUserResultCO=evaQueryGateway.getBeEvaTargetAmountUnqualifiedUser(null,num,target).get();
        }else {
            throw new SysException("type是10以外的值");
        }
        return unqualifiedUserResultCO;
    }
}
