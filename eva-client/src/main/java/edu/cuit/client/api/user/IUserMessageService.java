package edu.cuit.client.api.user;

import edu.cuit.client.dto.clientobject.EvaMsgCO;
import edu.cuit.client.dto.cmd.SendWarningMsgCmd;
import jakarta.validation.Valid;

import java.util.List;

/**
 * 消息相关业务接口
 */
public interface IUserMessageService {

    /**
     * 向评教者发送警告
     * @param cmd 信息模型
     */
    void sendWarningMessage(SendWarningMsgCmd cmd);

    /**
     * 获取当前用户的待办/通知
     * @param type 消息类型（-1或null：全部，0：待办，1：通知）
     */
    List<EvaMsgCO> getCurrentUserMsg(Integer type);

}
