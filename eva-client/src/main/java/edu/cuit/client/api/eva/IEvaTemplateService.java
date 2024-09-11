package edu.cuit.client.api.eva;

import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.clientobject.eva.EvaTaskFormCO;
import edu.cuit.client.dto.clientobject.eva.EvaTemplateCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;

import java.util.List;

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

    /**
     * 提交评教表单，完成评教任务
     * @param evaTaskFormCO 评教表单评价分值dto//返回数据类型原来没有刚建的
     */
    Void putEvaTemplate(EvaTaskFormCO evaTaskFormCO);
}
