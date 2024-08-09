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
 * 评教相关消息
 * @TableName eva_msg_tip
 */
@TableName(value ="eva_msg_tip")
@Data
public class EvaMsgTipDO implements Serializable {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 发起人的id(-1: 系统发起)
     */
    @TableField(value = "pro_id")
    private Integer pro_id;

    /**
     * 评教任务的id
     */
    @TableField(value = "task_id")
    private Integer task_id;

    /**
     * 具体提醒内容
     */
    @TableField(value = "msg")
    private String msg;

    /**
     * 消息类型（0：待办，1：通知）
     */
    @TableField(value = "type")
    private Integer type;

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