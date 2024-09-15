package edu.cuit.domain.entity;

import com.alibaba.cola.domain.Entity;
import edu.cuit.domain.entity.eva.EvaTaskEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户消息domain entity
 */
@Entity
@Data
@RequiredArgsConstructor
public class MsgEntity {

    /**
     * id
     */
    private Integer id;

    /**
     * 发起人(null: 系统发起)
     */
    private UserEntity sender;

    /**
     * 评教任务，当且仅当为评教消息时有意义
     */
    private EvaTaskEntity task;

    /**
     * 具体提醒内容
     */
    private String msg;

    /**
     * 消息类型（0：待办，1：通知，2：提醒，3：警告）
     */
    private Integer type;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 实现逻辑删除（0:不可用 1:可用）
     */
    private Integer isDeleted;

    /**
     * 接收消息的用户，为null则向所有人发送
     */
    private UserEntity recipient;

    /**
     * 确定是普通消息还是评教消息，0: 普通消息；1：评教消息
     */
    private Integer mode;

    /**
     * 发起者是否要进行匿名，1: 不匿名，0: 匿名
     */
    private Integer isShowName;

    /**
     * 确认该消息是否已读
     */
    private Integer isRead;

    /**
     * 该消息是否已经显示给接收者过了
     */
    private Integer isDisplayed;

}
