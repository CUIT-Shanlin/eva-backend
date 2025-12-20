package edu.cuit.infra.bcmessaging.adapter;

import com.alibaba.cola.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import edu.cuit.bc.messaging.application.port.MessageDisplayPort;
import edu.cuit.infra.dal.database.dataobject.MsgTipDO;
import edu.cuit.infra.dal.database.mapper.MsgTipMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * bc-messaging：消息显示状态端口适配器（保持历史行为不变：原样搬运旧 gateway 写流程）。
 */
@Component
@RequiredArgsConstructor
public class MessageDisplayPortImpl implements MessageDisplayPort {
    private final MsgTipMapper msgTipMapper;

    @Override
    public void updateDisplay(Integer userId, Integer id, Integer isDisplayed) {
        checkUser(userId, id);
        LambdaUpdateWrapper<MsgTipDO> msgUpdate = Wrappers.lambdaUpdate();
        msgUpdate.set(MsgTipDO::getIsDisplayed, isDisplayed)
                .eq(MsgTipDO::getRecipientId, userId)
                .eq(MsgTipDO::getId, id);
        msgTipMapper.update(msgUpdate);
    }

    private void checkUser(Integer userId, Integer id) {
        MsgTipDO msgTipDO = msgTipMapper.selectById(id);
        if (!Objects.equals(msgTipDO.getRecipientId(), userId)) {
            throw new BizException("只能修改自己的消息");
        }
    }
}

