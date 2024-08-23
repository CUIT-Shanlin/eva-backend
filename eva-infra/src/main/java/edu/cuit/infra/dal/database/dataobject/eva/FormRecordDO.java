package edu.cuit.infra.dal.database.dataobject.eva;

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
    private Integer taskId;

    /**
     * 详细的文字评价
     */
    @TableField(value = "text_value")
    private String textValue;

    /**
     * 表单评教指标以及对应的分值，JSON字符串格式的对象数组
     */
    @TableField(value = "form_props_values")
    private String formPropsValues;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private LocalDateTime createTime;

    /**
     * 实现逻辑删除（0:不可用 1:可用）
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    @TableField(exist = false)
    @Serial
    private static final long serialVersionUID = 1L;
}