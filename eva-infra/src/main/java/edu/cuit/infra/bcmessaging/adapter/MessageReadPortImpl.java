package edu.cuit.infra.bcmessaging.adapter;

import com.alibaba.cola.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import edu.cuit.bc.messaging.application.port.MessageReadPort;
import edu.cuit.infra.dal.database.dataobject.MsgTipDO;
import edu.cuit.infra.dal.database.mapper.MsgTipMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * bc-messaging：消息已读状态端口适配器（保持历史行为不变：原样搬运旧 gateway 写流程）。
 */
@Component
@RequiredArgsConstructor
public class MessageReadPortImpl implements MessageReadPort {
    private final MsgTipMapper msgTipMapper;

    @Override
    public void updateRead(Integer userId, Integer id, Integer isRead) {
        checkUser(userId, id);
        LambdaUpdateWrapper<MsgTipDO> msgUpdate = Wrappers.lambdaUpdate();
        msgUpdate.set(MsgTipDO::getIsRead, isRead)
                .eq(MsgTipDO::getRecipientId, userId)
                .eq(MsgTipDO::getId, id);
        msgTipMapper.update(msgUpdate);
    }

    @Override
    public void markAllRead(Integer userId, Integer mode) {
        LambdaUpdateWrapper<MsgTipDO> msgUpdate = Wrappers.lambdaUpdate();
        msgUpdate.set(MsgTipDO::getIsRead, 1)
                .eq(MsgTipDO::getRecipientId, userId)
                .eq(MsgTipDO::getMode, mode);
        msgTipMapper.update(msgUpdate);
    }

    private void checkUser(Integer userId, Integer id) {
        MsgTipDO msgTipDO = msgTipMapper.selectById(id);
        if (!Objects.equals(msgTipDO.getRecipientId(), userId)) {
            throw new BizException("只能修改自己的消息");
        }
    }
}

