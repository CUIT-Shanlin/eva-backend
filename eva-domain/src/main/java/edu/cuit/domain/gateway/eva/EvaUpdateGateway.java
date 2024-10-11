package edu.cuit.domain.gateway.eva;

import edu.cuit.client.dto.clientobject.eva.EvaInfoCO;
import edu.cuit.client.dto.clientobject.eva.EvaTaskFormCO;
import edu.cuit.client.dto.clientobject.eva.EvaTemplateCO;
import edu.cuit.domain.entity.eva.EvaTaskEntity;
import edu.cuit.domain.entity.eva.EvaTemplateEntity;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * 评教更新相关用户数据接口
 */
@Component
public interface EvaUpdateGateway {
    /**
     * 修改评教模板
     * @param evaTemplateCO EvaTemplateCO
     */
    Void updateEvaTemplate(EvaTemplateCO evaTemplateCO);
    /**
     * 提交评教表单，完成评教任务
     * @param evaTaskFormCO EvaTaskFormCO
     */
    Void putEvaTemplate(EvaTaskFormCO evaTaskFormCO);
    /**
     * 发起评教任务
     *@param evaInfoCO EvaInfoCO
     */
    Void postEvaTask(EvaInfoCO evaInfoCO);
    /**
     * 新建评教模板
     * @param evaTemplateCO EvaTemplateCO
     */
    Void addEvaTemplate(EvaTemplateCO evaTemplateCO);
    /**
     * 任意取消一个评教任务
     * @param id 任务id
     */
    Void cancelEvaTaskById(Integer id);

}
