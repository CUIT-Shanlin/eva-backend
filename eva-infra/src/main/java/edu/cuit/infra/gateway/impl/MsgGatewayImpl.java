package edu.cuit.infra.gateway.impl;

import edu.cuit.bc.messaging.application.usecase.DeleteMessageUseCase;
import edu.cuit.bc.messaging.application.usecase.InsertMessageUseCase;
import edu.cuit.bc.messaging.application.usecase.MarkMessageReadUseCase;
import edu.cuit.bc.messaging.application.usecase.QueryMessageUseCase;
import edu.cuit.bc.messaging.application.usecase.UpdateMessageDisplayUseCase;
import edu.cuit.client.dto.data.msg.GenericRequestMsg;
import edu.cuit.domain.entity.MsgEntity;
import edu.cuit.domain.gateway.MsgGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MsgGatewayImpl implements MsgGateway {

    private final DeleteMessageUseCase deleteMessageUseCase;

    private final MarkMessageReadUseCase markMessageReadUseCase;

    private final QueryMessageUseCase queryMessageUseCase;

    private final InsertMessageUseCase insertMessageUseCase;

    private final UpdateMessageDisplayUseCase updateMessageDisplayUseCase;

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
        updateMessageDisplayUseCase.updateDisplay(userId, id, isDisplayed);
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

}
