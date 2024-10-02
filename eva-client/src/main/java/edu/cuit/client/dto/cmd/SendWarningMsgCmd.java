package edu.cuit.client.dto.cmd;

import com.alibaba.cola.dto.DTO;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 评教信息消息（提醒用户）
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class SendWarningMsgCmd extends DTO {

    /**
     * 具体提醒内容
     */
    private String msg;

    /**
     * 发起人的id(-1: 系统发起)
     */
    @NotNull(message = "发起人的id不能为空")
    private Integer proId;

    /**
     * 评教任务的id
     */
    @NotNull(message = "评教任务id不能为空")
    private Integer taskId;

    /**
     * 是否显示发送者名称
     */
    @NotNull(message = "是否匿名属性不能空")
    private Boolean isShowName;

}
