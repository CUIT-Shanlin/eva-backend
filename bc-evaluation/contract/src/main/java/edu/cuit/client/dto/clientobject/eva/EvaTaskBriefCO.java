package edu.cuit.client.dto.clientobject.eva;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 评教任务最小视图：用于跨 BC 按 courInfIds 查询任务列表（仅携带必要字段）
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class EvaTaskBriefCO extends ClientObject {

    /**
     * 任务id
     */
    private Integer id;

    /**
     * 评教老师id（对应 eva_task.teacher_id）
     */
    private Integer teacherId;

    /**
     * 课程信息id（对应 eva_task.cour_inf_id）
     */
    private Integer courInfId;

    /**
     * 任务状态（0：待执行，1：已执行，2：已撤回）
     */
    private Integer status;
}

