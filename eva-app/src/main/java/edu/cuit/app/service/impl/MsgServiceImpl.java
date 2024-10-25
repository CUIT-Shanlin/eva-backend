package edu.cuit.app.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.cola.exception.BizException;
import com.alibaba.cola.exception.SysException;
import edu.cuit.app.convertor.MsgBizConvertor;
import edu.cuit.app.convertor.course.CourseBizConvertor;
import edu.cuit.app.websocket.WebsocketManager;
import edu.cuit.client.api.IMsgService;
import edu.cuit.client.bo.MessageBO;
import edu.cuit.client.dto.clientobject.course.SingleCourseCO;
import edu.cuit.client.dto.cmd.SendMessageCmd;
import edu.cuit.client.dto.data.msg.EvaResponseMsg;
import edu.cuit.client.dto.data.msg.GenericRequestMsg;
import edu.cuit.client.dto.data.msg.GenericResponseMsg;
import edu.cuit.domain.entity.MsgEntity;
import edu.cuit.domain.entity.course.SingleCourseEntity;
import edu.cuit.domain.gateway.MsgGateway;
import edu.cuit.domain.gateway.eva.EvaQueryGateway;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
@Slf4j
public class MsgServiceImpl implements IMsgService {

    private final MsgGateway msgGateway;
    private final UserQueryGateway userQueryGateway;
    private final EvaQueryGateway evaQueryGateway;

    private final WebsocketManager websocketManager;

    private final MsgBizConvertor msgBizConvertor;
    private final CourseBizConvertor courseBizConvertor;

    private final Executor executor;

    @Override
    @Transactional
    public List<GenericResponseMsg> getUserSelfNormalMsg(Integer type) {
        return  msgGateway.queryMsg(checkAndGetUserId(), type, 0).stream()
                .map(msgBizConvertor::toResponseMsg).toList();
    }

    @Override
    @Transactional
    public List<GenericResponseMsg> getUserTargetTypeMsg(Integer type, Integer mode) {
        List<GenericResponseMsg> result;
        if (mode == null || mode < 0) {
            result = new ArrayList<>(getUserSelfEvaMsg(type).stream()
                    .map(evaResponseMsg -> (GenericResponseMsg) evaResponseMsg)
                    .toList());
            result.addAll(getUserSelfNormalMsg(type));
        } else if (mode == 1) {
            result = getUserSelfEvaMsg(type).stream()
                    .map(evaResponseMsg -> (GenericResponseMsg) evaResponseMsg)
                    .toList();
        } else {
            result = getUserSelfNormalMsg(type);
        }
        return result;
    }

    @Override
    @Transactional
    public List<EvaResponseMsg> getUserSelfEvaMsg(Integer type) {
        List<MsgEntity> msgEntities = msgGateway.queryMsg(checkAndGetUserId(), type, 1);
        return msgEntities.stream().map(msgEntity -> evaQueryGateway.oneEvaTaskInfo(msgEntity.getTaskId()).map(taskEntity -> {
            // 获取评教信息对应课程
            SingleCourseEntity courInf = taskEntity.getCourInf();
            // 转换为课程对象
            SingleCourseCO singleCourseCO = courseBizConvertor.toSingleCourseCO(courInf,
                    evaQueryGateway.getEvaNumByCourInfo(courInf.getId()).orElse(0));
            return msgBizConvertor.toEvaResponseMsg(msgEntity,singleCourseCO);
        }).orElseThrow(() -> new BizException("获取评教信息失败"))).toList();
    }

    @Override
    @Transactional
    public List<GenericResponseMsg> getUserTargetAmountAndTypeMsg(Integer num, Integer type) {
        return msgGateway.queryTargetAmountMsg(checkAndGetUserId(),num,type).stream()
                .map(msgBizConvertor::toResponseMsg).toList();
    }

    @Override
    @Transactional
    public void updateMsgDisplay(Integer id, Integer isDisplayed) {
        msgGateway.updateMsgDisplay(checkAndGetUserId(),id,isDisplayed);
    }

    @Override
    @Transactional
    public void updateMsgRead(Integer id, Integer isRead) {
        msgGateway.updateMsgRead(checkAndGetUserId(),id,isRead);
    }

    @Override
    @Transactional
    public void updateMultipleMsgRead(Integer mode) {
        msgGateway.updateMultipleMsgRead(checkAndGetUserId(),mode);
    }

    @Override
    @Transactional
    public void deleteEvaMsg(Integer taskId, Integer type) {
        msgGateway.deleteMessage(taskId,type);
    }

    @Override
    @Transactional
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
            websocketManager.sendMessage(userQueryGateway.findUsernameById(msg.getRecipientId()).orElse(null),responseMsg);
            msgGateway.insertMessage(requestMsg);
        }
    }

    @Override
    @Transactional
    public void handleUserSendMessage(SendMessageCmd cmd) {
        sendMessage(msgBizConvertor.toMessageBO(cmd,checkAndGetUserId()));
    }

    private Integer checkAndGetUserId() {
        if (!StpUtil.isLogin()) {
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
