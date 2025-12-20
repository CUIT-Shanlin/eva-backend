package edu.cuit.bc.messaging.application.usecase;

import edu.cuit.bc.messaging.application.port.MessageDeletionPort;

import java.util.Objects;

/**
 * 删除消息用例（写模型入口）。
 *
 * <p>保持行为不变：用例仅做编排与依赖隔离，具体删除条件由端口实现原样搬运。</p>
 */
public class DeleteMessageUseCase {
    private final MessageDeletionPort deletionPort;

    public DeleteMessageUseCase(MessageDeletionPort deletionPort) {
        this.deletionPort = Objects.requireNonNull(deletionPort, "deletionPort");
    }

    public void deleteByTask(Integer taskId, Integer type) {
        deletionPort.deleteByTask(taskId, type);
    }

    public void deleteUserMessages(Integer userId, Integer mode, Integer type) {
        deletionPort.deleteUserMessages(userId, mode, type);
    }
}

