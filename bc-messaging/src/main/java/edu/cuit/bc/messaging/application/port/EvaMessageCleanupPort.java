package edu.cuit.bc.messaging.application.port;

/**
 * 评教相关消息清理端口（单体阶段复用现有 MsgService.deleteEvaMsg）。
 */
public interface EvaMessageCleanupPort {
    void deleteEvaMsg(Integer taskId);
}

