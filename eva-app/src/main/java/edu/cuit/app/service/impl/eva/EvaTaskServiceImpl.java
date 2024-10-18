package edu.cuit.app.service.impl.eva;
import com.alibaba.cola.exception.BizException;
import edu.cuit.app.aop.CheckSemId;
import edu.cuit.app.convertor.user.MenuBizConvertor;
import edu.cuit.client.api.eva.IEvaTaskService;
import edu.cuit.client.api.user.IMenuService;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.eva.EvaInfoCO;
import edu.cuit.client.dto.clientobject.eva.EvaTaskBaseInfoCO;
import edu.cuit.client.dto.clientobject.eva.EvaTaskDetailInfoCO;
import edu.cuit.client.dto.clientobject.user.GenericMenuSectionCO;
import edu.cuit.client.dto.clientobject.user.MenuCO;
import edu.cuit.client.dto.cmd.user.NewMenuCmd;
import edu.cuit.client.dto.cmd.user.UpdateMenuCmd;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.client.dto.query.condition.MenuConditionalQuery;
import edu.cuit.domain.entity.eva.EvaTaskEntity;
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
public class EvaTaskServiceImpl implements IEvaTaskService {
    private final EvaDeleteGateway evaDeleteGateway;
    private final EvaUpdateGateway evaUpdateGateway;
    private final EvaQueryGateway evaQueryGateway;
    @Override
    @CheckSemId
    public PaginationQueryResultCO<EvaTaskBaseInfoCO> pageEvaUnfinishedTask(Integer semId, PagingQuery<GenericConditionalQuery> query) {
        return null;
    }

    @Override
    @CheckSemId
    public List<EvaTaskDetailInfoCO> evaSelfTaskInfo(Integer semId, String keyword) {
        return null;
    }

    @Override
    public EvaTaskDetailInfoCO oneEvaTaskInfo(Integer id) {

        EvaTaskDetailInfoCO evaTaskDetailInfoCO=new EvaTaskDetailInfoCO();
        EvaTaskEntity evaTaskEntity=evaQueryGateway.oneEvaTaskInfo(id).get();

        return null;
    }

    @Override
    public Void postEvaTask(EvaInfoCO evaInfoCO) {
        return null;
    }
    //撤回消息
    @Override
    public Void cancelEvaTask(Integer id) {
        return null;
    }
    //怎么判断自己的
    @Override
    public Void cancelMyEvaTask(Integer id) {
        return null;
    }
}
