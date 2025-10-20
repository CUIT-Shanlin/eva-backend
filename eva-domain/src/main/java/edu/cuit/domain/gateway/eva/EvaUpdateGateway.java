package edu.cuit.domain.gateway.eva;

import edu.cuit.client.dto.cmd.eva.EvaTemplateCmd;
import edu.cuit.client.dto.cmd.eva.NewEvaLogCmd;
import edu.cuit.client.dto.cmd.eva.NewEvaTaskCmd;
import edu.cuit.client.dto.cmd.eva.NewEvaTemplateCmd;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.List;

/**
 * 评教更新相关用户数据接口
 */
@Component
public interface EvaUpdateGateway {
    /**
     * 修改评教模板
     * @param evaTemplateCmd EvaTemplateCmd
     */
    Void updateEvaTemplate(EvaTemplateCmd evaTemplateCmd);
    /**
     * 提交评教表单，完成评教任务记得完成评教任务之后，
     * 要删除对应的两种消息 “该任务的待办评教消息” “该任务的系统逾期提醒消息”
     * 改：对应的评价不能低于50字
     * @param newEvaLogCmd NewEvaLogCmd
     */
    Integer putEvaTemplate(NewEvaLogCmd newEvaLogCmd);
    /**
     * 发起评教任务
     * 要同时发送该任务的评教待办消息;
     *@param newEvaTaskCmd NewEvaTaskCmd
     * @param maxNum 最大泡脚数目
     */
    Integer postEvaTask(NewEvaTaskCmd newEvaTaskCmd,Integer maxNum);
    /**
     * 新建评教模板
     * @param newEvaTemplateCmd NewEvaTemplateCmd
     */
    Void addEvaTemplate(NewEvaTemplateCmd newEvaTemplateCmd) throws ParseException;
    /**
     * 任意取消一个评教任务
     * @param id 任务id
     */
    Void cancelEvaTaskById(Integer id);

}
