package edu.cuit.infra.bcmessaging.adapter;

import edu.cuit.bc.messaging.application.port.MessageInsertionPort;
import edu.cuit.client.dto.data.msg.GenericRequestMsg;
import edu.cuit.infra.convertor.MsgConvertor;
import edu.cuit.infra.dal.database.dataobject.MsgTipDO;
import edu.cuit.infra.dal.database.mapper.MsgTipMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * bc-messaging：消息新增端口适配器（保持历史行为不变：原样搬运旧 gateway 写流程）。
 */
@Component
@RequiredArgsConstructor
public class MessageInsertionPortImpl implements MessageInsertionPort {
    private final MsgTipMapper msgTipMapper;
    private final MsgConvertor msgConvertor;

    @Override
    public void insertMessage(GenericRequestMsg msg) {
        MsgTipDO msgDO = msgConvertor.toMsgDO(msg);
        msgTipMapper.insert(msgDO);
        msg.setId(msgDO.getId());
        msg.setCreateTime(LocalDateTime.now());
    }
}
