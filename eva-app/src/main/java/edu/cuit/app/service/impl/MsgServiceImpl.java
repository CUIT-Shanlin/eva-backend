package edu.cuit.app.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.cola.exception.BizException;
import com.alibaba.cola.exception.SysException;
import edu.cuit.app.convertor.MsgBizConvertor;
import edu.cuit.app.websocket.WebsocketManager;
import edu.cuit.client.api.IMsgService;
import edu.cuit.client.bo.MessageBO;
import edu.cuit.client.dto.clientobject.course.SingleCourseCO;
import edu.cuit.client.dto.cmd.SendMessageCmd;
import edu.cuit.client.dto.data.msg.GenericRequestMsg;
import edu.cuit.client.dto.data.msg.GenericResponseMsg;
import edu.cuit.domain.gateway.MsgGateway;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
@Slf4j
public class MsgServiceImpl implements IMsgService {

    private final MsgGateway msgGateway;
    private final UserQueryGateway userQueryGateway;

    private final WebsocketManager websocketManager;

    private final MsgBizConvertor msgBizConvertor;

    private final Executor executor;

    @Override
    public List<GenericResponseMsg> getUserTargetTypeMsg(Integer type, Integer mode, SingleCourseCO courseInfo) {
        List<GenericResponseMsg> result;
        if (mode == 1) {
            result = msgGateway.queryMsg(checkAndGetUserId(), type, mode).stream()
                    .map(msg -> ((GenericResponseMsg) msgBizConvertor.toEvaResponseMsg(msg, courseInfo))).toList();
        } else {
            result = msgGateway.queryMsg(checkAndGetUserId(), type, mode).stream()
                    .map(msgBizConvertor::toResponseMsg).toList();
        }
        return result;
    }

    @Override
    public List<GenericResponseMsg> getUserTargetAmountAndTypeMsg(Integer num, Integer type) {
        return msgGateway.queryTargetAmountMsg(checkAndGetUserId(),num,type).stream()
                .map(msgBizConvertor::toResponseMsg).toList();
    }

    @Override
    public void updateMsgDisplay(Integer id, Integer isDisplayed) {
        msgGateway.updateMsgDisplay(checkAndGetUserId(),id,isDisplayed);
    }

    @Override
    public void updateMsgRead(Integer id, Integer isRead) {
        msgGateway.updateMsgRead(checkAndGetUserId(),id,isRead);
    }

    @Override
    public void updateMultipleMsgRead(Integer mode) {
        msgGateway.updateMultipleMsgRead(checkAndGetUserId(),mode);
    }

    @Override
    public void sendMessage(MessageBO msg) {
        String senderName = null;
        if (msg.getIsShowName() == 1) {
            if (msg.getSenderId() != null && msg.getSenderId() >= 0) {
                senderName = userQueryGateway.findUsernameById(msg.getSenderId())
                        .orElseThrow(() -> {
                            SysException e = new SysException("查找发送者用户信息失败，请联系管理员");
                            log.error("查找发送者用户信息失败，请联系管理员",e);
                            return e;
                        });
            } else senderName = "系统";
        }
        GenericRequestMsg requestMsg = msgBizConvertor.toRequestMsg(msg);
        GenericResponseMsg responseMsg = msgBizConvertor.toResponseMsg(requestMsg, senderName);
        // 判断是否为广播消息
        if (msg.getRecipientId() == null || msg.getRecipientId() < 0) {
            // 异步处理
            CompletableFuture.runAsync(() -> {
                for (Integer id : userQueryGateway.findAllUserId()) {
                    GenericRequestMsg cloneMsg = ObjectUtil.clone(requestMsg);
                    cloneMsg.setRecipientId(id);
                    msgGateway.insertMessage(cloneMsg);
                }
                websocketManager.broadcastMessage(responseMsg);
            },executor);

        } else {
            websocketManager.sendMessage(msg.getRecipientId(),responseMsg);
            msgGateway.insertMessage(requestMsg);
        }
    }

    @Override
    public void handleUserSendMessage(SendMessageCmd cmd) {
        sendMessage(msgBizConvertor.toMessageBO(cmd,checkAndGetUserId()));
    }

    private Integer checkAndGetUserId() {
        if (StpUtil.isLogin()) {
            throw new BizException("用户未登录");
        }
        String loginId = (String) StpUtil.getLoginId();
        return userQueryGateway.findIdByUsername(loginId)
                .orElseThrow(() -> {
                    SysException sysException = new SysException("因系统原因验证失败，请联系管理员");
                    log.error("因系统原因验证失败，请联系管理员", sysException);
                    return sysException;
                });
    }
}
