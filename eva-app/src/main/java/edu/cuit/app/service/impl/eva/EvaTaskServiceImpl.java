package edu.cuit.app.service.impl.eva;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.cola.exception.BizException;
import com.alibaba.cola.exception.SysException;
import edu.cuit.bc.evaluation.application.model.PostEvaTaskCommand;
import edu.cuit.bc.evaluation.application.usecase.EvaTaskQueryUseCase;
import edu.cuit.bc.evaluation.application.usecase.PostEvaTaskUseCase;
import edu.cuit.bc.evaluation.domain.PostEvaTaskQueryException;
import edu.cuit.bc.evaluation.domain.PostEvaTaskUpdateException;
import edu.cuit.app.aop.CheckSemId;
import edu.cuit.app.service.impl.MsgServiceImpl;
import edu.cuit.client.api.eva.IEvaTaskService;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.eva.EvaTaskBaseInfoCO;
import edu.cuit.client.dto.clientobject.eva.EvaTaskDetailInfoCO;
import edu.cuit.client.dto.cmd.eva.NewEvaTaskCmd;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.EvaTaskConditionalQuery;
import edu.cuit.domain.entity.eva.EvaTaskEntity;
import edu.cuit.domain.gateway.course.CourseQueryGateway;
import edu.cuit.domain.gateway.eva.EvaConfigGateway;
import edu.cuit.domain.gateway.eva.EvaDeleteGateway;
import edu.cuit.bc.evaluation.application.port.EvaTaskInfoQueryPort;
import edu.cuit.domain.gateway.eva.EvaUpdateGateway;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EvaTaskServiceImpl implements IEvaTaskService {
    private final EvaTaskQueryUseCase evaTaskQueryUseCase;
    private final EvaUpdateGateway evaUpdateGateway;
    private final EvaTaskInfoQueryPort evaTaskInfoQueryPort;
    private final EvaDeleteGateway evaDeleteGateway;
    private final UserQueryGateway userQueryGateway;
    private final CourseQueryGateway courseQueryGateway;
    private final EvaConfigGateway evaConfigGateway;
    private final MsgServiceImpl msgService;
    private final PostEvaTaskUseCase postEvaTaskUseCase;
    @Override
    @CheckSemId
    public PaginationQueryResultCO<EvaTaskBaseInfoCO> pageEvaUnfinishedTask(Integer semId, PagingQuery<EvaTaskConditionalQuery> query) {
        return evaTaskQueryUseCase.pageEvaUnfinishedTaskAsPaginationQueryResult(semId, query);
    }

    @Override
    @CheckSemId
    public List<EvaTaskDetailInfoCO> evaSelfTaskInfo(Integer semId, String keyword) {
        Integer useId=userQueryGateway.findIdByUsername(String.valueOf(StpUtil.getLoginId())).orElseThrow(()->new SysException("并没有找到该用户id"));
        return evaTaskQueryUseCase.evaSelfTaskInfo(useId, semId, keyword);
    }

    @Override
    public EvaTaskDetailInfoCO oneEvaTaskInfo(Integer id) {
        return evaTaskQueryUseCase.oneEvaTaskInfo(id);
    }
    //发起任务之后，要同时发送该任务的评教待办消息
    @Override
    @Transactional
    public Void postEvaTask(NewEvaTaskCmd newEvaTaskCmd) {
        Integer maxTaskNum= evaConfigGateway.getMaxBeEvaNum();
        try {
            postEvaTaskUseCase.post(new PostEvaTaskCommand(newEvaTaskCmd.getCourInfId(), newEvaTaskCmd.getTeacherId()), maxTaskNum);
        } catch (PostEvaTaskUpdateException e) {
            throw new UpdateException(e.getMessage());
        } catch (PostEvaTaskQueryException e) {
            throw new QueryException(e.getMessage());
        }
        return null;
    }

    @Override
    public Void cancelEvaTask(Integer id) {
        LogUtils.logContent(evaTaskInfoQueryPort.getNameByTaskId(id).orElseThrow(() -> new BizException("该任务id不存在")) + "任务ID为 "+id+" 的评教任务");
        evaUpdateGateway.cancelEvaTaskById(id);
        msgService.deleteEvaMsg(id,null);
        return null;
    }
    @Override
    public Void cancelMyEvaTask(Integer id) {
        Integer useId=userQueryGateway.findIdByUsername(String.valueOf(StpUtil.getLoginId())).orElseThrow(() -> new SysException("用户未找到"));
        EvaTaskEntity evaTaskEntity=evaTaskInfoQueryPort.oneEvaTaskInfo(id).orElseThrow(() -> new BizException("该任务不存在"));
        if(!Objects.equals(evaTaskEntity.getTeacher().getId(), useId)){
            throw new QueryException("不能删去不是自己评教的任务");
        }else{
            evaUpdateGateway.cancelEvaTaskById(id);
            msgService.deleteEvaMsg(id,null);
        }
        return null;
    }

    @Override
    public Void deleteAllTaskByTea(Integer teacherId) {
        List<Integer> evaTaskDOIds=evaDeleteGateway.deleteAllTaskByTea(teacherId);
        if(evaTaskDOIds.size()!=0) {
            for (int i = 0; i < evaTaskDOIds.size(); i++) {
                msgService.deleteEvaMsg(evaTaskDOIds.get(i), null);
            }
        }
        return null;
    }
}
