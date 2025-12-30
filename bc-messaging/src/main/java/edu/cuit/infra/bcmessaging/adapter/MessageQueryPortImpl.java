package edu.cuit.infra.bcmessaging.adapter;

import com.alibaba.cola.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import edu.cuit.bc.messaging.application.port.MessageQueryPort;
import edu.cuit.domain.entity.MsgEntity;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import edu.cuit.infra.convertor.MsgConvertor;
import edu.cuit.infra.dal.database.dataobject.MsgTipDO;
import edu.cuit.infra.dal.database.mapper.MsgTipMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * bc-messaging：消息查询端口适配器（保持历史行为不变：原样搬运旧 gateway 查询/组装逻辑）。
 */
@Component
@RequiredArgsConstructor
public class MessageQueryPortImpl implements MessageQueryPort {
    private final MsgTipMapper msgTipMapper;
    private final UserQueryGateway userQueryGateway;
    private final MsgConvertor msgConvertor;

    @Override
    public List<MsgEntity> queryMsg(Integer userId, Integer type, Integer mode) {
        LambdaQueryWrapper<MsgTipDO> msgQuery = Wrappers.lambdaQuery();
        msgQuery.eq(MsgTipDO::getRecipientId, userId);
        if (type != null && type >= 0) msgQuery.eq(MsgTipDO::getType, type);
        if (mode != null && mode >= 0) msgQuery.eq(MsgTipDO::getMode, mode);
        msgQuery.orderByDesc(MsgTipDO::getCreateTime);
        return msgTipMapper.selectList(msgQuery).stream()
                .map(this::getMsgEntity)
                .toList();
    }

    @Override
    public List<MsgEntity> queryTargetAmountMsg(Integer userId, Integer num, Integer type) {
        LambdaQueryWrapper<MsgTipDO> msgQuery = Wrappers.lambdaQuery();
        msgQuery.eq(MsgTipDO::getRecipientId, userId);
        if (type != null && type >= 0) msgQuery.eq(MsgTipDO::getType, type);
        if (num != null && num >= 0) msgQuery.last("limit " + num);
        msgQuery.orderByDesc(MsgTipDO::getCreateTime);
        return msgTipMapper.selectList(msgQuery).stream()
                .map(this::getMsgEntity)
                .toList();
    }

    private MsgEntity getMsgEntity(MsgTipDO msgTipDO) {
        return msgConvertor.toMsgEntity(msgTipDO,
                () -> {
                    if (msgTipDO.getSenderId() == null || msgTipDO.getSenderId() < 0) {
                        return null;
                    }
                    return userQueryGateway.findById(msgTipDO.getSenderId())
                            .orElseThrow(() -> new BizException("发送者id不存在"));
                },
                () -> userQueryGateway.findById(msgTipDO.getRecipientId())
                        .orElseThrow(() -> new BizException("接受者id不存在")));
    }
}
