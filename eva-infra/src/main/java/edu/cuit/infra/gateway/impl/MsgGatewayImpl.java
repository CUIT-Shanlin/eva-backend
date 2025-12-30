package edu.cuit.infra.gateway.impl;

import edu.cuit.client.dto.data.msg.GenericRequestMsg;
import edu.cuit.domain.entity.MsgEntity;
import edu.cuit.domain.gateway.MsgGateway;
import edu.cuit.domain.gateway.msg.MessageUseCaseFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MsgGatewayImpl implements MsgGateway {

    private final MessageUseCaseFacade messageUseCaseFacade;

    @Override
    public List<MsgEntity> queryMsg(Integer userId, Integer type, Integer mode) {
        return messageUseCaseFacade.queryMsg(userId, type, mode);
    }

    @Override
    public List<MsgEntity> queryTargetAmountMsg(Integer userId, Integer num, Integer type) {
        return messageUseCaseFacade.queryTargetAmountMsg(userId, num, type);
    }

    @Override
    public void updateMsgDisplay(Integer userId, Integer id, Integer isDisplayed) {
        messageUseCaseFacade.updateDisplay(userId, id, isDisplayed);
    }

    @Override
    public void updateMsgRead(Integer userId,Integer id, Integer isRead) {
        messageUseCaseFacade.updateRead(userId, id, isRead);
    }

    @Override
    public void updateMultipleMsgRead(Integer userId,Integer mode) {
        messageUseCaseFacade.markAllRead(userId, mode);
    }

    @Override
    public void insertMessage(GenericRequestMsg msg) {
        messageUseCaseFacade.insertMessage(msg);
    }

    @Override
    public void deleteMessage(Integer taskId, Integer type) {
        messageUseCaseFacade.deleteByTask(taskId, type);
    }

    @Override
    public void deleteTargetTypeMessage(Integer userId,Integer mode, Integer type) {
        messageUseCaseFacade.deleteUserMessages(userId, mode, type);
    }

}
