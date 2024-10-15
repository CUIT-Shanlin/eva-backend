package edu.cuit.domain.gateway;

import edu.cuit.domain.entity.MsgEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 消息数据门户
 */
@Component
public interface MsgGateway {

    /**
     * 查询消息
     * @param userId 用户id
     * @param type 消息类型（0：待办，1：通知，2：提醒，3：警告；null或者负数：全部）
     * @param mode 确定是普通消息还是评教消息，0: 普通消息；1：评教消息，null或者负数：全部
     */
    List<MsgEntity> queryMsg(Integer userId, Integer type, Integer mode);

    /**
     *
     * @param userId 用户id
     * @param num 指定消息数目，（负数或者null：全部）
     * @param type 消息类型（0：待办，1：通知，2：提醒，3：警告；null或者负数：全部）
     */
    List<MsgEntity> queryTargetAmountMsg(Integer userId,Integer num,Integer type);

    /**
     * 修改某条消息的显示状态
     * @param userId 用户id
     * @param id 消息id
     * @param isDisplayed 待改成的显示状态，0：未显示过，1：已显示过
     */
    void updateMsgDisplay(Integer userId,Integer id,Integer isDisplayed);

    /**
     * 修改某条消息的已读状态
     * @param userId 用户id
     * @param id 消息id
     * @param isRead 待改成的已读状态，0：未读，1：已读
     */
    void updateMsgRead(Integer userId,Integer id,Integer isRead);

    /**
     * 批量修改某种性质的消息的已读状态，（注：改为已读的同时，也要改为已显示）
     * @param userId 用户id
     * @param mode 确定待批量修改的是普通消息还是评教消息，0: 普通消息；1：评教消息
     */
    void updateMultipleMsgRead(Integer userId,Integer mode);
}
