package edu.cuit.bc.messaging.application.usecase;

import edu.cuit.bc.messaging.application.port.MessageReadPort;

import java.util.Objects;

/**
 * 标记消息已读用例（写模型入口）。
 *
 * <p>保持行为不变：用例仅做编排与依赖隔离，具体校验与更新逻辑在端口适配器中原样搬运。</p>
 */
public class MarkMessageReadUseCase {
    private final MessageReadPort readPort;

    public MarkMessageReadUseCase(MessageReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort");
    }

    public void updateRead(Integer userId, Integer id, Integer isRead) {
        readPort.updateRead(userId, id, isRead);
    }

    public void markAllRead(Integer userId, Integer mode) {
        readPort.markAllRead(userId, mode);
    }
}

