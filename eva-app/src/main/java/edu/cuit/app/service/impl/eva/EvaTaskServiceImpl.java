package edu.cuit.app.service.impl.eva;
import cn.dev33.satoken.stp.StpUtil;
import edu.cuit.app.aop.CheckSemId;
import edu.cuit.app.convertor.eva.EvaTaskBizConvertor;
import edu.cuit.client.api.eva.IEvaTaskService;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.eva.EvaInfoCO;
import edu.cuit.client.dto.clientobject.eva.EvaTaskBaseInfoCO;
import edu.cuit.client.dto.clientobject.eva.EvaTaskDetailInfoCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.course.SingleCourseEntity;
import edu.cuit.domain.entity.eva.EvaTaskEntity;
import edu.cuit.domain.gateway.eva.EvaDeleteGateway;
import edu.cuit.domain.gateway.eva.EvaQueryGateway;
import edu.cuit.domain.gateway.eva.EvaUpdateGateway;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaTaskServiceImpl implements IEvaTaskService {
    private final EvaDeleteGateway evaDeleteGateway;
    private final EvaUpdateGateway evaUpdateGateway;
    private final EvaQueryGateway evaQueryGateway;
    private final EvaTaskBizConvertor evaTaskBizConvertor;
    @Override
    @CheckSemId
    public PaginationQueryResultCO<EvaTaskBaseInfoCO> pageEvaUnfinishedTask(Integer semId, PagingQuery<GenericConditionalQuery> query) {
        return null;
    }

    @Override
    @CheckSemId
    public List<EvaTaskDetailInfoCO> evaSelfTaskInfo(Integer semId, String keyword) {
        Integer useId=(Integer) StpUtil.getLoginId();
        List<EvaTaskEntity> evaTaskEntities=evaQueryGateway.evaSelfTaskInfo(useId,semId,keyword);
        List<EvaTaskDetailInfoCO> evaTaskDetailInfoCOS=new ArrayList<>();
        for(int i=0;i<evaTaskEntities.size();i++) {
            SingleCourseEntity singleCourseEntity = evaTaskEntities.get(i).getCourInf();
            EvaTaskDetailInfoCO evaTaskDetailInfoCO = evaTaskBizConvertor.evaTaskEntityToTaskDetailCO(evaTaskEntities.get(i), singleCourseEntity);
            evaTaskDetailInfoCOS.add(evaTaskDetailInfoCO);
        }
        return null;
    }

    @Override
    public EvaTaskDetailInfoCO oneEvaTaskInfo(Integer id) {
        EvaTaskEntity evaTaskEntity=evaQueryGateway.oneEvaTaskInfo(id).get();
        SingleCourseEntity singleCourseEntity=evaTaskEntity.getCourInf();
        EvaTaskDetailInfoCO evaTaskDetailInfoCO=evaTaskBizConvertor.evaTaskEntityToTaskDetailCO(evaTaskEntity,singleCourseEntity);
        return evaTaskDetailInfoCO;
    }
    //发起任务之后，要同时发送该任务的评教待办消息 TODO
    @Override
    public Void postEvaTask(EvaInfoCO evaInfoCO) {
        evaUpdateGateway.postEvaTask(evaInfoCO);
        return null;
    }

    @Override
    public Void cancelEvaTask(Integer id) {
        evaUpdateGateway.cancelEvaTaskById(id);
        return null;
    }
    @Override
    public Void cancelMyEvaTask(Integer id) {
        Integer useId=(Integer) StpUtil.getLoginId();
        EvaTaskEntity evaTaskEntity=evaQueryGateway.oneEvaTaskInfo(id).get();
        if(evaTaskEntity.getTeacher().getId()!=useId){
            throw new QueryException("不能删去不是自己评教的任务");
        }else{
            evaUpdateGateway.cancelEvaTaskById(id);
        }
        return null;
    }
}
