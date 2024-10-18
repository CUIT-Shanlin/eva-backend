package edu.cuit.app.service.impl.eva;

import edu.cuit.app.aop.CheckSemId;
import edu.cuit.client.api.eva.IEvaRecordService;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.eva.EvaRecordCO;
import edu.cuit.client.dto.clientobject.eva.EvaTaskFormCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.EvaLogConditionalQuery;
import edu.cuit.domain.gateway.eva.EvaDeleteGateway;
import edu.cuit.domain.gateway.eva.EvaQueryGateway;
import edu.cuit.domain.gateway.eva.EvaUpdateGateway;
import edu.cuit.infra.convertor.eva.EvaConvertor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaRecordServiceImpl implements IEvaRecordService {
    private final EvaDeleteGateway evaDeleteGateway;
    private final EvaUpdateGateway evaUpdateGateway;
    private final EvaQueryGateway evaQueryGateway;
    private final EvaConvertor convertor;
    @Override
    @CheckSemId
    public PaginationQueryResultCO<EvaRecordCO> pageEvaRecord(Integer semId, PagingQuery<EvaLogConditionalQuery> query) {
        return null;
    }

    @Override
    public Void deleteOneEvaLogById(Integer id) {
        List<Integer> list=new ArrayList<>();
        list.add(id);
        evaDeleteGateway.deleteEvaRecord(list);
        return null;
    }

    @Override
    public Void deleteEvaLogsById(List<Integer> ids) {
        evaDeleteGateway.deleteEvaRecord(ids);
        return null;
    }
    //记得完成评教任务之后，要删除对应的两种消息 “该任务的待办评教消息” “该任务的系统逾期提醒消息”
    @Override
    public Void putEvaTemplate(EvaTaskFormCO evaTaskFormCO) {
        evaUpdateGateway.putEvaTemplate(evaTaskFormCO);
        return null;
    }
}
