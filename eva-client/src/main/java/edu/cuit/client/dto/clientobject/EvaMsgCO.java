package edu.cuit.client.dto.clientobject;

import com.alibaba.cola.dto.ClientObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户评教消息
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class EvaMsgCO extends ClientObject {

    /**
     * id
     */
    private Integer id;

    /**
     * 具体提醒内容
     */
    private String msg;

    /**
     * 消息类型（0：待办，1：通知）
     */
    private Integer type;

    /**
     * 评教任务id
     */
    private  Integer taskId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 发起人的名称
     */
    private String proName;

}
