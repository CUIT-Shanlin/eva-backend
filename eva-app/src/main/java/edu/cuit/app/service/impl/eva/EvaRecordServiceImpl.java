package edu.cuit.app.service.impl.eva;

import com.alibaba.cola.exception.SysException;
import edu.cuit.bc.evaluation.application.model.FormPropValue;
import edu.cuit.bc.evaluation.application.model.SubmitEvaluationCommand;
import edu.cuit.bc.evaluation.application.usecase.DeleteEvaRecordUseCase;
import edu.cuit.bc.evaluation.application.usecase.SubmitEvaluationUseCase;
import edu.cuit.bc.evaluation.domain.DeleteEvaRecordQueryException;
import edu.cuit.bc.evaluation.domain.DeleteEvaRecordUpdateException;
import edu.cuit.bc.evaluation.domain.SubmitEvaluationException;
import edu.cuit.app.aop.CheckSemId;
import edu.cuit.app.convertor.PaginationBizConvertor;
import edu.cuit.app.convertor.eva.EvaRecordBizConvertor;
import edu.cuit.client.api.eva.IEvaRecordService;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.eva.EvaRecordCO;
import edu.cuit.client.dto.cmd.eva.NewEvaLogCmd;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.EvaLogConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.eva.EvaRecordEntity;
import edu.cuit.bc.evaluation.application.port.EvaRecordPagingQueryPort;
import edu.cuit.bc.evaluation.application.port.EvaRecordScoreQueryPort;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaRecordServiceImpl implements IEvaRecordService {
    private final EvaRecordPagingQueryPort evaRecordPagingQueryPort;
    private final EvaRecordScoreQueryPort evaRecordScoreQueryPort;
    private final EvaRecordBizConvertor evaRecordBizConvertor;
    private final SubmitEvaluationUseCase submitEvaluationUseCase;
    private final DeleteEvaRecordUseCase deleteEvaRecordUseCase;
    @Override
    @CheckSemId
    public PaginationQueryResultCO<EvaRecordCO> pageEvaRecord(Integer semId, PagingQuery<EvaLogConditionalQuery> query) {
        PaginationResultEntity<EvaRecordEntity> page=evaRecordPagingQueryPort.pageEvaRecord(semId,query);
        List<EvaRecordCO> results = page.getRecords().stream()
                .map(evaRecordBizConvertor::evaRecordEntityToCo)
                .toList();
        for(int i=0;i<results.size();i++){
            results.get(i).setAverScore(evaRecordScoreQueryPort.getScoreFromRecord(page.getRecords().get(i).getFormPropsValues()).orElseThrow(()->new SysException("相关模板不存在")));
        }
        PaginationQueryResultCO<EvaRecordCO> pageCO = new PaginationQueryResultCO<>();
        pageCO.setCurrent(page.getCurrent())
                .setSize(page.getSize())
                .setTotal(page.getTotal())
                .setRecords(results);
        return pageCO;
    }

    @Override
    public Void deleteOneEvaLogById(Integer id) {
        List<Integer> list=new ArrayList<>();
        list.add(id);
        try {
            deleteEvaRecordUseCase.delete(list);
        } catch (DeleteEvaRecordQueryException e) {
            throw new QueryException(e.getMessage());
        } catch (DeleteEvaRecordUpdateException e) {
            throw new UpdateException(e.getMessage());
        }
        return null;
    }

    @Override
    public Void deleteEvaLogsById(List<Integer> ids) {
        try {
            deleteEvaRecordUseCase.delete(ids);
        } catch (DeleteEvaRecordQueryException e) {
            throw new QueryException(e.getMessage());
        } catch (DeleteEvaRecordUpdateException e) {
            throw new UpdateException(e.getMessage());
        }
        return null;
    }
    //记得完成评教任务之后，
    // 要删除对应的两种消息 “该任务的待办评教消息” “该任务的系统逾期提醒消息”
    @Override
    @Transactional
    public Void putEvaTemplate(NewEvaLogCmd newEvaLogCmd) {
        List<FormPropValue> formPropsValues = newEvaLogCmd.getFormPropsValues() == null
                ? null
                : newEvaLogCmd.getFormPropsValues().stream()
                .map(p -> new FormPropValue(p.getProp(), p.getScore()))
                .toList();
        try {
            submitEvaluationUseCase.submit(new SubmitEvaluationCommand(
                    newEvaLogCmd.getTaskId(),
                    newEvaLogCmd.getTextValue(),
                    formPropsValues
            ));
        } catch (SubmitEvaluationException e) {
            throw new UpdateException(e.getMessage());
        }
        return null;
    }
}
