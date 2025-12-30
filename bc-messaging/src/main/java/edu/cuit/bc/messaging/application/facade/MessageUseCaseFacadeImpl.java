package edu.cuit.bc.messaging.application.facade;

import edu.cuit.bc.messaging.application.usecase.DeleteMessageUseCase;
import edu.cuit.bc.messaging.application.usecase.InsertMessageUseCase;
import edu.cuit.bc.messaging.application.usecase.MarkMessageReadUseCase;
import edu.cuit.bc.messaging.application.usecase.QueryMessageUseCase;
import edu.cuit.bc.messaging.application.usecase.UpdateMessageDisplayUseCase;
import edu.cuit.client.dto.data.msg.GenericRequestMsg;
import edu.cuit.domain.entity.MsgEntity;
import edu.cuit.domain.gateway.msg.MessageUseCaseFacade;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 消息域用例门面实现（保持行为不变：仅做调用转发，不引入新逻辑）。
 */
@Component
@RequiredArgsConstructor
public class MessageUseCaseFacadeImpl implements MessageUseCaseFacade {

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
    public void updateDisplay(Integer userId, Integer id, Integer isDisplayed) {
        updateMessageDisplayUseCase.updateDisplay(userId, id, isDisplayed);
    }

    @Override
    public void updateRead(Integer userId, Integer id, Integer isRead) {
        markMessageReadUseCase.updateRead(userId, id, isRead);
    }

    @Override
    public void markAllRead(Integer userId, Integer mode) {
        markMessageReadUseCase.markAllRead(userId, mode);
    }

    @Override
    public void insertMessage(GenericRequestMsg msg) {
        insertMessageUseCase.insertMessage(msg);
    }

    @Override
    public void deleteByTask(Integer taskId, Integer type) {
        deleteMessageUseCase.deleteByTask(taskId, type);
    }

    @Override
    public void deleteUserMessages(Integer userId, Integer mode, Integer type) {
        deleteMessageUseCase.deleteUserMessages(userId, mode, type);
    }
}

