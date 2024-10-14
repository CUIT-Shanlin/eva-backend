package edu.cuit.infra.dal.database.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 消息表
 * @TableName msg_tip
 */
@TableName(value ="msg_tip")
@Data
@Accessors(chain = true)
public class MsgTipDO implements Serializable {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 发起人的id(-1或null: 系统发起)
     */
    @TableField(value = "sender _id")
    private Integer senderId;

    /**
     * 评教任务的id，当且仅当为评教消息时有意义
     */
    @TableField(value = "task_id")
    private Integer taskId;

    /**
     * 具体提醒内容
     */
    @TableField(value = "msg")
    private String msg;

    /**
     * 消息类型（0：待办，1：通知，2：提醒，3：警告）
     */
    @TableField(value = "type")
    private Integer type;

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

    /**
     * 接收消息的用户的id，为null则向所有人发送
     */
    @TableField(value = "recipient_id")
    private Integer recipientId;

    /**
     * 确定是普通消息还是评教消息，0: 普通消息；1：评教消息
     */
    @TableField(value = "mode")
    private Integer mode;

    /**
     * 发起者是否要进行匿名，true: 不匿名，false: 匿名
     */
    @TableField(value = "is_show_name")
    private Integer isShowName;

    /**
     * 确认该消息是否已读
     */
    @TableField(value = "is_read")
    private Integer isRead;

    /**
     * 该消息是否已经显示给接收者过了
     */
    @TableField(value = "is_displayed")
    private Integer isDisplayed;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}