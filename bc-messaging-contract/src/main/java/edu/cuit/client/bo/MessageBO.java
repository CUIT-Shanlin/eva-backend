package edu.cuit.client.bo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 系统内创建的消息对象
 */
@Data
@Accessors(chain = true)
public class MessageBO {

    /**
     * 发起者是否要进行匿名，1: 不匿名，0: 匿名
     */
    private Integer isShowName;

    /**
     * 确定是普通消息还是评教消息，0: 普通消息；1：评教消息
     */
    private Integer mode;

    /**
     * 评教任务的id，当且仅当为评教消息时有意义
     */
    private Integer taskId;

    /**
     * 具体提醒内容
     */
    private String msg;

    /**
     * 接收消息的用户的id，为null则向所有人发送
     */
    private Integer recipientId;

    /**
     * 发起人的id(-1或null: 系统发起)
     */
    private Integer senderId;

    /**
     * 消息类型（0：待办，1：通知，2：提醒，3：警告）
     */
    private Integer type;

}
