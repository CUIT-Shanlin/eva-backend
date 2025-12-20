package edu.cuit.infra.bcmessaging.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import edu.cuit.bc.messaging.application.port.MessageDeletionPort;
import edu.cuit.infra.dal.database.dataobject.MsgTipDO;
import edu.cuit.infra.dal.database.mapper.MsgTipMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * bc-messaging：消息删除端口适配器（保持历史行为不变：原样搬运旧 gateway 删除条件）。
 */
@Component
@RequiredArgsConstructor
public class MessageDeletionPortImpl implements MessageDeletionPort {
    private final MsgTipMapper msgTipMapper;

    @Override
    public void deleteByTask(Integer taskId, Integer type) {
        LambdaQueryWrapper<MsgTipDO> msgQuery = Wrappers.lambdaQuery();
        msgQuery.eq(MsgTipDO::getTaskId, taskId);
        if (type != null && type >= 0) {
            msgQuery.eq(MsgTipDO::getType, type);
        }
        msgTipMapper.delete(msgQuery);
    }

    @Override
    public void deleteUserMessages(Integer userId, Integer mode, Integer type) {
        LambdaQueryWrapper<MsgTipDO> msgQuery = Wrappers.lambdaQuery();
        msgQuery.eq(MsgTipDO::getRecipientId, userId);
        if (mode != null && mode >= 0) {
            msgQuery.eq(MsgTipDO::getMode, mode);
        }
        if (type != null && type >= 0) {
            msgQuery.eq(MsgTipDO::getType, type);
        }
        msgTipMapper.delete(msgQuery);
    }
}
