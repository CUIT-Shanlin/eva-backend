package edu.cuit.infra.dal.po.eva;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 评教表单记录
 * @TableName form_record
 */
@TableName(value ="form_record")
@Data
public class FormRecordDO implements Serializable {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 任务id
     */
    @TableField(value = "task_id")
    private Integer task_id;

    /**
     * 表单模板id
     */
    @TableField(value = "template_id")
    private Integer template_id;

    /**
     * 详细的文字评价
     */
    @TableField(value = "text_value")
    private String text_value;

    /**
     * 表单评教指标以及对应的分值，JSON字符串格式的对象数组
     */
    @TableField(value = "form_props_values")
    private String form_props_values;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private LocalDateTime create_time;

    /**
     * 实现逻辑删除（0:不可用 1:可用）
     */
    @TableField(value = "is_deleted")
    private Integer is_deleted;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}