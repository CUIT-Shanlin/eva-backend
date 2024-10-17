package edu.cuit.client.api.eva;

import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.eva.EvaTaskFormCO;
import edu.cuit.client.dto.clientobject.eva.EvaTemplateCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;

import java.util.List;
import java.util.Optional;

/**
 * 评教模板相关业务接口
 */
public interface IEvaTemplateService {
    //查询

    /**
     *分页获取评教模板信息
     * @param semId 学期id
     * @param query 查询dto
     */
    PaginationQueryResultCO<EvaTemplateCO> pageEvaTemplate(Integer semId, PagingQuery<GenericConditionalQuery> query);

    /**
     * 获取所有模板的基础信息，仅包含名称和id信息
     */
    List<SimpleResultCO> evaAllTemplate ();
    /**
     * 获取一个任务对应的评教模板，任务id拿到这节课的id，这节课的id拿这门课的id，然后可以拿到对应的“模板的快照表”中的评教模板，
     * 注：要拿快照中的模板，不要直接去找“评教模板表”中的模板。
     * @param taskId 任务id
     * @param semId 学期id
     */
    Optional<String> evaTemplateByTaskId(Integer taskId, Integer semId);

    //修改

    /**
     * 删除评教模板
     * @param templateId 模板的 ID 编号
     */
    Void deleteEvaTemplateById(Integer templateId);

    /**
     * 批量删除模板，后端要再次检验是否可被删除
     * @param ids 模板数组
     */
    Void deleteEvaTemplatesById(List<Integer> ids);

    /**
     * 修改评教模板，注：只有在该评教模板没有分配在课程中 且 评教记录中也没使用过该模板 才可以进行删除或修改！
     * @param evaTemplateCO 评教模板dto
     */
    Void updateEvaTemplate(EvaTemplateCO evaTemplateCO);

    /**
     * 新建评教模板
     * @param evaTemplateCO 评教模板dto
     */
    Void addEvaTemplate(EvaTemplateCO evaTemplateCO);


}
