package edu.cuit.infra.gateway.impl;

import com.alibaba.cola.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import edu.cuit.bc.messaging.application.usecase.DeleteMessageUseCase;
import edu.cuit.client.dto.data.msg.GenericRequestMsg;
import edu.cuit.domain.entity.MsgEntity;
import edu.cuit.domain.gateway.MsgGateway;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import edu.cuit.infra.convertor.MsgConvertor;
import edu.cuit.infra.dal.database.dataobject.MsgTipDO;
import edu.cuit.infra.dal.database.mapper.MsgTipMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class MsgGatewayImpl implements MsgGateway {

    private final MsgTipMapper msgTipMapper;

    private final UserQueryGateway userQueryGateway;

    private final MsgConvertor msgConvertor;

    private final DeleteMessageUseCase deleteMessageUseCase;

    @Override
    public List<MsgEntity> queryMsg(Integer userId, Integer type, Integer mode) {
        LambdaQueryWrapper<MsgTipDO> msgQuery = Wrappers.lambdaQuery();
        msgQuery.eq(MsgTipDO::getRecipientId,userId);
        if (type != null && type >= 0) msgQuery.eq(MsgTipDO::getType,type);
        if (mode != null && mode >= 0) msgQuery.eq(MsgTipDO::getMode,mode);
        msgQuery.orderByDesc(MsgTipDO::getCreateTime);
        return msgTipMapper.selectList(msgQuery).stream()
                .map(this::getMsgEntity)
                .toList();
    }

    @Override
    public List<MsgEntity> queryTargetAmountMsg(Integer userId, Integer num, Integer type) {
        LambdaQueryWrapper<MsgTipDO> msgQuery = Wrappers.lambdaQuery();
        msgQuery.eq(MsgTipDO::getRecipientId,userId);
        if (type != null && type >= 0) msgQuery.eq(MsgTipDO::getType,type);
        if (num != null && num >= 0) msgQuery.last("limit " + num);
        msgQuery.orderByDesc(MsgTipDO::getCreateTime);
        return msgTipMapper.selectList(msgQuery).stream()
                .map(this::getMsgEntity)
                .toList();
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
        checkUser(userId,id);
        LambdaUpdateWrapper<MsgTipDO> msgUpdate = Wrappers.lambdaUpdate();
        msgUpdate.set(MsgTipDO::getIsRead,isRead)
                .eq(MsgTipDO::getRecipientId,userId)
                .eq(MsgTipDO::getId,id);
        msgTipMapper.update(msgUpdate);
    }

    @Override
    public void updateMultipleMsgRead(Integer userId,Integer mode) {
        LambdaUpdateWrapper<MsgTipDO> msgUpdate = Wrappers.lambdaUpdate();
        msgUpdate.set(MsgTipDO::getIsRead,1)
                .eq(MsgTipDO::getRecipientId,userId)
                .eq(MsgTipDO::getMode,mode);
        msgTipMapper.update(msgUpdate);
    }

    @Override
    public void insertMessage(GenericRequestMsg msg) {
        MsgTipDO msgDO = msgConvertor.toMsgDO(msg);
        msgTipMapper.insert(msgDO);
        msg.setId(msgDO.getId());
        msg.setCreateTime(LocalDateTime.now());
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

    private MsgEntity getMsgEntity(MsgTipDO msgTipDO) {
        return msgConvertor.toMsgEntity(msgTipDO,
                () -> {
                    if (msgTipDO.getSenderId() == null || msgTipDO.getSenderId() < 0) {
                        return null;
                    }
                    return userQueryGateway.findById(msgTipDO.getSenderId()).orElseThrow(() -> new BizException("发送者id不存在"));
                },
                () -> userQueryGateway.findById(msgTipDO.getRecipientId()).orElseThrow(() -> new BizException("接受者id不存在")));
    }
}
