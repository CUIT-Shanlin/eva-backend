package edu.cuit.app.service.impl.eva;

import com.alibaba.cola.exception.SysException;
import edu.cuit.app.aop.CheckSemId;
import edu.cuit.app.convertor.PaginationBizConvertor;
import edu.cuit.app.convertor.eva.EvaRecordBizConvertor;
import edu.cuit.app.service.impl.MsgServiceImpl;
import edu.cuit.client.api.eva.IEvaRecordService;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.eva.EvaRecordCO;
import edu.cuit.client.dto.clientobject.eva.EvaTaskFormCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.EvaLogConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.eva.EvaRecordEntity;
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
public class EvaRecordServiceImpl implements IEvaRecordService {
    private final EvaDeleteGateway evaDeleteGateway;
    private final EvaUpdateGateway evaUpdateGateway;
    private final EvaQueryGateway evaQueryGateway;
    private final EvaRecordBizConvertor evaRecordBizConvertor;
    private final PaginationBizConvertor paginationBizConvertor;
    private final MsgServiceImpl msgService;
    @Override
    @CheckSemId
    public PaginationQueryResultCO<EvaRecordCO> pageEvaRecord(Integer semId, PagingQuery<EvaLogConditionalQuery> query) {
        PaginationResultEntity<EvaRecordEntity> page=evaQueryGateway.pageEvaRecord(semId,query);
        List<EvaRecordCO> results = page.getRecords().stream()
                .map(evaRecordBizConvertor::evaRecordEntityToCo)
                .toList();
        for(int i=0;i<results.size();i++){
            results.get(i).setAverScore(evaQueryGateway.getScoreFromRecord(page.getRecords().get(i).getFormPropsValues()).orElseThrow(()->new SysException("相关模板不存在")));
        }
        return paginationBizConvertor.toPaginationEntity(page,results);
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
    //记得完成评教任务之后，
    // 要删除对应的两种消息 “该任务的待办评教消息” “该任务的系统逾期提醒消息”
    @Override
    public Void putEvaTemplate(EvaTaskFormCO evaTaskFormCO) {
        evaUpdateGateway.putEvaTemplate(evaTaskFormCO);
        //删除所有相关消息
        msgService.deleteEvaMsg(evaTaskFormCO.getTaskId(),null);
        return null;
    }
}
