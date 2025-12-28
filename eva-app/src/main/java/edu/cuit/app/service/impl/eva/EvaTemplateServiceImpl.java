package edu.cuit.app.service.impl.eva;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import edu.cuit.bc.evaluation.application.model.AddEvaTemplateCommand;
import edu.cuit.bc.evaluation.application.model.UpdateEvaTemplateCommand;
import edu.cuit.bc.evaluation.application.usecase.AddEvaTemplateUseCase;
import edu.cuit.bc.evaluation.application.usecase.DeleteEvaTemplateUseCase;
import edu.cuit.bc.evaluation.application.usecase.UpdateEvaTemplateUseCase;
import edu.cuit.bc.evaluation.domain.AddEvaTemplateUpdateException;
import edu.cuit.bc.evaluation.domain.DeleteEvaTemplateQueryException;
import edu.cuit.bc.evaluation.domain.DeleteEvaTemplateUpdateException;
import edu.cuit.bc.evaluation.domain.UpdateEvaTemplateUpdateException;
import edu.cuit.app.aop.CheckSemId;
import edu.cuit.app.convertor.PaginationBizConvertor;
import edu.cuit.client.api.eva.IEvaTemplateService;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.eva.EvaTemplateCO;
import edu.cuit.client.dto.cmd.eva.EvaTemplateCmd;
import edu.cuit.client.dto.cmd.eva.NewEvaTemplateCmd;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.eva.EvaTemplateEntity;
import edu.cuit.bc.evaluation.application.port.EvaTemplateAllQueryPort;
import edu.cuit.bc.evaluation.application.port.EvaTemplatePagingQueryPort;
import edu.cuit.bc.evaluation.application.port.EvaTemplateTaskTemplateQueryPort;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaTemplateServiceImpl implements IEvaTemplateService {
    private final EvaTemplatePagingQueryPort evaTemplatePagingQueryPort;
    private final EvaTemplateAllQueryPort evaTemplateAllQueryPort;
    private final EvaTemplateTaskTemplateQueryPort evaTemplateTaskTemplateQueryPort;
    private final PaginationBizConvertor paginationBizConvertor;
    private final DeleteEvaTemplateUseCase deleteEvaTemplateUseCase;
    private final AddEvaTemplateUseCase addEvaTemplateUseCase;
    private final UpdateEvaTemplateUseCase updateEvaTemplateUseCase;
    @Override
    @CheckSemId
    public PaginationQueryResultCO<EvaTemplateCO> pageEvaTemplate(Integer semId, PagingQuery<GenericConditionalQuery> query) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        PaginationResultEntity<EvaTemplateEntity> page=evaTemplatePagingQueryPort.pageEvaTemplate(semId,query);
        List<EvaTemplateCO> results =new ArrayList<>();
        for(int i=0;i<page.getRecords().size();i++){
            EvaTemplateCO evaTemplateCO=new EvaTemplateCO();
            evaTemplateCO.setId(page.getRecords().get(i).getId());
            evaTemplateCO.setName(page.getRecords().get(i).getName());
            evaTemplateCO.setDescription(page.getRecords().get(i).getDescription());
            evaTemplateCO.setUpdateTime(page.getRecords().get(i).getUpdateTime().format(fmt));
            evaTemplateCO.setCreateTime(page.getRecords().get(i).getCreateTime().format(fmt));
            evaTemplateCO.setProps(page.getRecords().get(i).getProps());
            evaTemplateCO.setIsDefault(page.getRecords().get(i).getIsDefault());

            results.add(evaTemplateCO);
        }
        return paginationBizConvertor.toPaginationEntity(page,results);
    }

    @Override
    public List<SimpleResultCO> evaAllTemplate() {
        List<EvaTemplateEntity> evaTemplateEntities=evaTemplateAllQueryPort.getAllTemplate();
        if(CollectionUtil.isEmpty(evaTemplateEntities)){
            List list=new ArrayList();
            return list;
        }
        List<SimpleResultCO> simpleResultCOS=new ArrayList<>();
        for(int i=0;i<evaTemplateEntities.size();i++){
            SimpleResultCO simpleResultCO=new SimpleResultCO();
            simpleResultCO.setId(evaTemplateEntities.get(i).getId());
            simpleResultCO.setName(evaTemplateEntities.get(i).getName());
            simpleResultCOS.add(i,simpleResultCO);
        }
        return simpleResultCOS;
    }

    @Override
    @CheckSemId
    public String evaTemplateByTaskId(Integer taskId, Integer semId) {
        return evaTemplateTaskTemplateQueryPort.getTaskTemplate(taskId,semId).orElseGet(() -> JSONUtil.toJsonStr(List.of()));
    }

    @Override
    public Void deleteEvaTemplateById(Integer templateId) {
        List<Integer> list=new ArrayList<>();
        list.add(templateId);
        try {
            deleteEvaTemplateUseCase.delete(list);
        } catch (DeleteEvaTemplateQueryException e) {
            throw new QueryException(e.getMessage());
        } catch (DeleteEvaTemplateUpdateException e) {
            throw new UpdateException(e.getMessage());
        }
        return null;
    }
    //该评教模板没有分配在课程中才可以进行删除或修改！在gateway里面实现
    @Override
    public Void deleteEvaTemplatesById(List<Integer> ids) {
        try {
            deleteEvaTemplateUseCase.delete(ids);
        } catch (DeleteEvaTemplateQueryException e) {
            throw new QueryException(e.getMessage());
        } catch (DeleteEvaTemplateUpdateException e) {
            throw new UpdateException(e.getMessage());
        }
        return null;
    }

    @Override
    public Void updateEvaTemplate(EvaTemplateCmd evaTemplateCmd) {
        try {
            updateEvaTemplateUseCase.update(new UpdateEvaTemplateCommand(
                    evaTemplateCmd.getId(),
                    evaTemplateCmd.getName(),
                    evaTemplateCmd.getDescription(),
                    evaTemplateCmd.getProps()
            ));
        } catch (UpdateEvaTemplateUpdateException e) {
            throw new UpdateException(e.getMessage());
        }
        return null;
    }

    @Override
    public Void addEvaTemplate(NewEvaTemplateCmd newEvaTemplateCmd) throws ParseException {
        try {
            addEvaTemplateUseCase.add(new AddEvaTemplateCommand(
                    newEvaTemplateCmd.getName(),
                    newEvaTemplateCmd.getDescription(),
                    newEvaTemplateCmd.getProps()
            ));
        } catch (AddEvaTemplateUpdateException e) {
            throw new UpdateException(e.getMessage());
        }
        return null;
    }
}
