package edu.cuit.client.api;

import edu.cuit.client.bo.MessageBO;
import edu.cuit.client.dto.clientobject.course.SingleCourseCO;
import edu.cuit.client.dto.cmd.SendMessageCmd;
import edu.cuit.client.dto.data.msg.EvaResponseMsg;
import edu.cuit.client.dto.data.msg.GenericResponseMsg;

import java.util.List;

/**
 * 消息相关接口
 */
public interface IMsgService {

    /**
     * 获取当前用户指定类型的消息
     * @param type 消息类型（0：待办，1：通知，2：提醒，3：警告；null或者负数：全部）
     * @param mode 确定是普通消息还是评教消息，0: 普通消息；1：评教消息，null或者负数：全部
     */
    List<GenericResponseMsg> getUserTargetTypeMsg(Integer type,Integer mode);

    /**
     * 获取当前用户自己的指定类型的所有消息
     * @param type 消息类型（0：待办，1：通知，2：提醒，3：警告；null或者负数：全部）
     */
    List<GenericResponseMsg> getUserSelfNormalMsg(Integer type);

    /**
     * 获取当前用户的评教消息
     * @param type 消息类型（0：待办，1：通知，2：提醒，3：警告；null或者负数：全部）
     */
    List<EvaResponseMsg> getUserSelfEvaMsg(Integer type);

    /**
     * 获取用户自己指定数目的指定类型的消息
     * @param num 指定消息数目，（负数或者null：全部）
     * @param type 消息类型（0：待办，1：通知，2：提醒，3：警告；null或者负数：全部）
     */
    List<GenericResponseMsg> getUserTargetAmountAndTypeMsg(Integer num, Integer type);

    /**
     * 修改当前用户的某条消息的显示状态
     * @param id 消息id
     * @param isDisplayed 待改成的显示状态，0：未显示过，1：已显示过
     */
    void updateMsgDisplay(Integer id, Integer isDisplayed);

    /**
     * 修改当前用户的某条消息的已读状态
     * @param id 消息id
     * @param isRead 待改成的已读状态，0：未读，1：已读
     */
    void updateMsgRead(Integer id, Integer isRead);

    /**
     * 批量修改当前用户的某种性质的消息的已读状态，（注：改为已读的同时，也要改为已显示）
     * @param mode 确定待批量修改的是普通消息还是评教消息，0: 普通消息；1：评教消息
     */
    void updateMultipleMsgRead(Integer mode);

    /**
     * 发送消息
     * @param msg 请求消息对象
     */
    void sendMessage(MessageBO msg);

    /**
     * 处理用户发送消息
     * @param cmd 消息对象
     */
    void handleUserSendMessage(SendMessageCmd cmd);

}
