package edu.cuit.app.service.impl.eva;
import cn.dev33.satoken.stp.StpUtil;
import edu.cuit.app.aop.CheckSemId;
import edu.cuit.app.convertor.PaginationBizConvertor;
import edu.cuit.app.convertor.eva.EvaTaskBizConvertor;
import edu.cuit.app.service.impl.MsgServiceImpl;
import edu.cuit.client.api.eva.IEvaTaskService;
import edu.cuit.client.bo.MessageBO;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.eva.EvaInfoCO;
import edu.cuit.client.dto.clientobject.eva.EvaTaskBaseInfoCO;
import edu.cuit.client.dto.clientobject.eva.EvaTaskDetailInfoCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.EvaTaskConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.course.SingleCourseEntity;
import edu.cuit.domain.entity.eva.EvaTaskEntity;
import edu.cuit.domain.gateway.course.CourseQueryGateway;
import edu.cuit.domain.gateway.eva.EvaQueryGateway;
import edu.cuit.domain.gateway.eva.EvaUpdateGateway;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaTaskServiceImpl implements IEvaTaskService {
    private final EvaUpdateGateway evaUpdateGateway;
    private final EvaQueryGateway evaQueryGateway;
    private final UserQueryGateway userQueryGateway;
    private final CourseQueryGateway courseQueryGateway;
    private final EvaTaskBizConvertor evaTaskBizConvertor;
    private final PaginationBizConvertor paginationBizConvertor;
    private final MsgServiceImpl msgService;
    @Override
    @CheckSemId
    public PaginationQueryResultCO<EvaTaskBaseInfoCO> pageEvaUnfinishedTask(Integer semId, PagingQuery<EvaTaskConditionalQuery> query) {
        PaginationResultEntity<EvaTaskEntity> page=evaQueryGateway.pageEvaUnfinishedTask(semId,query);
        List<EvaTaskBaseInfoCO> results =page.getRecords().stream()
                .map(evaTaskBizConvertor::evaTaskEntityToEvaBaseCO)
                .toList();
        return paginationBizConvertor.toPaginationEntity(page,results);
    }

    @Override
    @CheckSemId
    public List<EvaTaskDetailInfoCO> evaSelfTaskInfo(Integer semId, String keyword) {
        Integer useId=userQueryGateway.findIdByUsername(String.valueOf(StpUtil.getLoginId())).get();
        List<EvaTaskEntity> evaTaskEntities=evaQueryGateway.evaSelfTaskInfo(useId,semId,keyword);
        List<EvaTaskDetailInfoCO> evaTaskDetailInfoCOS=new ArrayList<>();
        for (EvaTaskEntity evaTaskEntity : evaTaskEntities) {
            SingleCourseEntity singleCourseEntity = evaTaskEntity.getCourInf();
            EvaTaskDetailInfoCO evaTaskDetailInfoCO = evaTaskBizConvertor.evaTaskEntityToTaskDetailCO(evaTaskEntity, singleCourseEntity);
            evaTaskDetailInfoCOS.add(evaTaskDetailInfoCO);
        }
        return evaTaskDetailInfoCOS;
    }

    @Override
    public EvaTaskDetailInfoCO oneEvaTaskInfo(Integer id) {
        EvaTaskEntity evaTaskEntity=evaQueryGateway.oneEvaTaskInfo(id).get();
        SingleCourseEntity singleCourseEntity=evaTaskEntity.getCourInf();
        return evaTaskBizConvertor.evaTaskEntityToTaskDetailCO(evaTaskEntity,singleCourseEntity);
    }
    //发起任务之后，要同时发送该任务的评教待办消息
    @Override
    public Void postEvaTask(EvaInfoCO evaInfoCO) {
        String msg=evaUpdateGateway.postEvaTask(evaInfoCO);
        msgService.sendMessage(new MessageBO().setMsg(msg)
                .setMode(1).setIsShowName(1)
                .setRecipientId(evaInfoCO.getTeacherId()).setSenderId(evaInfoCO.getTeacherId())
                .setType(0).setTaskId(evaInfoCO.getId()));
        return null;
    }

    @Override
    public Void cancelEvaTask(Integer id) {
        evaUpdateGateway.cancelEvaTaskById(id);
        return null;
    }
    @Override
    public Void cancelMyEvaTask(Integer id) {
        Integer useId=userQueryGateway.findIdByUsername(String.valueOf(StpUtil.getLoginId())).get();
        EvaTaskEntity evaTaskEntity=evaQueryGateway.oneEvaTaskInfo(id).get();
        if(evaTaskEntity.getTeacher().getId()!=useId){
            throw new QueryException("不能删去不是自己评教的任务");
        }else{
            evaUpdateGateway.cancelEvaTaskById(id);
        }
        return null;
    }
}
