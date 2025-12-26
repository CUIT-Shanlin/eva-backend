package edu.cuit.client.dto.cmd;

import edu.cuit.client.validator.status.ValidStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 系统内创建的消息对象
 */
@Data
@Accessors(chain = true)
public class SendMessageCmd {

    /**
     * 发起者是否要进行匿名，1: 不匿名，0: 匿名
     */
    @ValidStatus(message = "是否匿名只能为0或1")
    @NotNull(message = "是否匿名不能为空")
    private Integer isShowName;

    /**
     * 确定是普通消息还是评教消息，0: 普通消息；1：评教消息
     */
    @ValidStatus(message = "消息mode只能为0或1")
    @NotNull(message = "消息mode不能为空")
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
     * 消息类型（0：待办，1：通知，2：提醒，3：警告）
     */
    @ValidStatus(value = {0,1,2,3},message = "消息类型只能为0、1、2或3")
    @NotNull(message = "消息类型不能为空")
    private Integer type;

}
