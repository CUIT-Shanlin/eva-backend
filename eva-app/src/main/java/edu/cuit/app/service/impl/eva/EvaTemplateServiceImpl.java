package edu.cuit.app.service.impl.eva;
import edu.cuit.app.aop.CheckSemId;
import edu.cuit.app.convertor.PaginationBizConvertor;
import edu.cuit.app.convertor.eva.EvaTemplateBizConvertor;
import edu.cuit.client.api.eva.IEvaTemplateService;
import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.eva.EvaTemplateCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.eva.EvaTemplateEntity;
import edu.cuit.domain.gateway.eva.EvaDeleteGateway;
import edu.cuit.domain.gateway.eva.EvaQueryGateway;
import edu.cuit.domain.gateway.eva.EvaUpdateGateway;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EvaTemplateServiceImpl implements IEvaTemplateService {
    private final EvaDeleteGateway evaDeleteGateway;
    private final EvaUpdateGateway evaUpdateGateway;
    private final EvaQueryGateway evaQueryGateway;
    private final PaginationBizConvertor paginationBizConvertor;
    private final EvaTemplateBizConvertor evaTemplateBizConvertor;
    @Override
    @CheckSemId
    public PaginationQueryResultCO<EvaTemplateCO> pageEvaTemplate(Integer semId, PagingQuery<GenericConditionalQuery> query) {
        PaginationResultEntity<EvaTemplateEntity> page=evaQueryGateway.pageEvaTemplate(semId,query);
        List<EvaTemplateCO> results = page.getRecords().stream()
                .map(evaTemplateBizConvertor::evaTemplateToEvaTemplateEntity)
                .toList();
        return paginationBizConvertor.toPaginationEntity(page,results);
    }

    @Override
    public List<SimpleResultCO> evaAllTemplate() {
        List<EvaTemplateEntity> evaTemplateEntities=evaQueryGateway.getAllTemplate();
        if(evaTemplateEntities==null){
            throw new QueryException("暂时还没有评教模板");
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
        return evaQueryGateway.getTaskTemplate(taskId,semId).get();
    }

    @Override
    public Void deleteEvaTemplateById(Integer templateId) {
        List<Integer> list=new ArrayList<>();
        list.add(templateId);
        evaDeleteGateway.deleteEvaTemplate(list);
        return null;
    }
    //该评教模板没有分配在课程中才可以进行删除或修改！在gateway里面实现
    @Override
    public Void deleteEvaTemplatesById(List<Integer> ids) {
        evaDeleteGateway.deleteEvaTemplate(ids);
        return null;
    }

    @Override
    public Void updateEvaTemplate(EvaTemplateCO evaTemplateCO) {
        evaUpdateGateway.updateEvaTemplate(evaTemplateCO);
        return null;
    }

    @Override
    public Void addEvaTemplate(EvaTemplateCO evaTemplateCO) {
        evaUpdateGateway.addEvaTemplate(evaTemplateCO);
        return null;
    }
}
