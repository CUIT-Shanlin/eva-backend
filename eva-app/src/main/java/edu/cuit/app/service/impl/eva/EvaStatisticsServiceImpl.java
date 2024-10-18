package edu.cuit.app.service.impl.eva;
import com.alibaba.cola.exception.BizException;
import edu.cuit.app.aop.CheckSemId;
import edu.cuit.app.convertor.user.MenuBizConvertor;
import edu.cuit.client.api.eva.IEvaStatisticsService;
import edu.cuit.client.api.user.IMenuService;
import edu.cuit.client.dto.clientobject.eva.*;
import edu.cuit.client.dto.clientobject.user.GenericMenuSectionCO;
import edu.cuit.client.dto.clientobject.user.MenuCO;
import edu.cuit.client.dto.cmd.user.NewMenuCmd;
import edu.cuit.client.dto.cmd.user.UpdateMenuCmd;
import edu.cuit.client.dto.query.condition.MenuConditionalQuery;
import edu.cuit.domain.gateway.eva.EvaDeleteGateway;
import edu.cuit.domain.gateway.eva.EvaQueryGateway;
import edu.cuit.domain.gateway.eva.EvaUpdateGateway;
import edu.cuit.domain.gateway.user.MenuQueryGateway;
import edu.cuit.domain.gateway.user.MenuUpdateGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaStatisticsServiceImpl implements IEvaStatisticsService {
    private final EvaDeleteGateway evaDeleteGateway;
    private final EvaUpdateGateway evaUpdateGateway;
    private final EvaQueryGateway evaQueryGateway;
    @Override
    @CheckSemId
    public EvaScoreInfoCO evaScoreStatisticsInfo(Integer semId, Number score) {
        return null;
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
    public ScoreRangeCourseCO scoreRangeCourseInfo(Integer num, Integer interval) {
        return null;
    }

    @Override
    @CheckSemId
    public List<Integer> getMonthEvaNUmber(Integer semId) {
        return null;
    }

    @Override
    @CheckSemId
    public PastTimeEvaDetailCO getEvaData(Integer semId, Integer num, Integer target, Integer evaTarget) {
        return null;
    }
}
