package edu.cuit.infra.gateway.impl;

import com.alibaba.cola.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import edu.cuit.bc.messaging.application.usecase.DeleteMessageUseCase;
import edu.cuit.bc.messaging.application.usecase.InsertMessageUseCase;
import edu.cuit.bc.messaging.application.usecase.MarkMessageReadUseCase;
import edu.cuit.bc.messaging.application.usecase.QueryMessageUseCase;
import edu.cuit.client.dto.data.msg.GenericRequestMsg;
import edu.cuit.domain.entity.MsgEntity;
import edu.cuit.domain.gateway.MsgGateway;
import edu.cuit.infra.dal.database.dataobject.MsgTipDO;
import edu.cuit.infra.dal.database.mapper.MsgTipMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class MsgGatewayImpl implements MsgGateway {

    private final MsgTipMapper msgTipMapper;

    private final DeleteMessageUseCase deleteMessageUseCase;

    private final MarkMessageReadUseCase markMessageReadUseCase;

    private final QueryMessageUseCase queryMessageUseCase;

    private final InsertMessageUseCase insertMessageUseCase;

    @Override
    public List<MsgEntity> queryMsg(Integer userId, Integer type, Integer mode) {
        return queryMessageUseCase.queryMsg(userId, type, mode);
    }

    @Override
    public List<MsgEntity> queryTargetAmountMsg(Integer userId, Integer num, Integer type) {
        return queryMessageUseCase.queryTargetAmountMsg(userId, num, type);
    }

    @Override
    public void updateMsgDisplay(Integer userId, Integer id, Integer isDisplayed) {
        checkUser(userId,id);
        LambdaUpdateWrapper<MsgTipDO> msgUpdate = Wrappers.lambdaUpdate();
        msgUpdate.set(MsgTipDO::getIsDisplayed,isDisplayed)
                .eq(MsgTipDO::getRecipientId,userId)
                .eq(MsgTipDO::getId,id);
        msgTipMapper.update(msgUpdate);
    }

    @Override
    public void updateMsgRead(Integer userId,Integer id, Integer isRead) {
        markMessageReadUseCase.updateRead(userId, id, isRead);
    }

    @Override
    public void updateMultipleMsgRead(Integer userId,Integer mode) {
        markMessageReadUseCase.markAllRead(userId, mode);
    }

    @Override
    public void insertMessage(GenericRequestMsg msg) {
        insertMessageUseCase.insertMessage(msg);
    }

    @Override
    public void deleteMessage(Integer taskId, Integer type) {
        deleteMessageUseCase.deleteByTask(taskId, type);
    }

    @Override
    public void deleteTargetTypeMessage(Integer userId,Integer mode, Integer type) {
        deleteMessageUseCase.deleteUserMessages(userId, mode, type);
    }

    private void checkUser(Integer userId, Integer id) {
        MsgTipDO msgTipDO = msgTipMapper.selectById(id);
        if (!Objects.equals(msgTipDO.getRecipientId(), userId)) {
            throw new BizException("只能修改自己的消息");
        }
    }

}
