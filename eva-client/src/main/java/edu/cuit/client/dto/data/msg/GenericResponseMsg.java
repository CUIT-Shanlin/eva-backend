package edu.cuit.client.dto.data.msg;

import com.alibaba.cola.dto.DTO;
import edu.cuit.client.dto.clientobject.course.SingleCourseCO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 服务端返回评教消息
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class GenericResponseMsg extends DTO {

    /**
     * id
     */
    private Integer id;

    /**
     * 该消息是否已经显示给接收者过了，0：未显示过，1：已显示过
     */
    private Integer isDisplayed;

    /**
     * 确认该消息是否已读，0：未读，1：已读
     */
    private Integer isRead;

    /**
     * 发起者是否要进行匿名，1: 不匿名，0: 匿名
     */
    private Integer isShowName;

    /**
     * 确定是普通消息还是评教消息，0: 普通消息；1：评教消息
     */
    private Integer mode;

    /**
     * 具体提醒内容
     */
    private String msg;

    /**
     * 接收消息的用户的id，为null则向所有人发送
     */
    private Long recipientId;

    /**
     * 消息类型（0：待办，1：通知，2：提醒，3：警告）
     */
    private Integer type;


    /**
     * 发起人的id(-1或null: 系统发起)
     */
    private Long senderId;

    /**
     * 评教任务的id，当且仅当为评教消息时有意义
     */
    private Long taskId;

    /**
     * 消息发送时间
     */
    private LocalDateTime createTime;

    /**
     * 发起人的名称，发起人的名称
     */
    private String senderName;
}
