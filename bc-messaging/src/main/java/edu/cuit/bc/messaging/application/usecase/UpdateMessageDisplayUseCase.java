package edu.cuit.bc.messaging.application.usecase;

import edu.cuit.bc.messaging.application.port.MessageDisplayPort;

import java.util.Objects;

/**
 * 更新消息展示状态用例（写模型入口）。
 *
 * <p>保持行为不变：用例仅做编排与依赖隔离，具体校验与更新逻辑在端口适配器中原样搬运。</p>
 */
public class UpdateMessageDisplayUseCase {
    private final MessageDisplayPort displayPort;

    public UpdateMessageDisplayUseCase(MessageDisplayPort displayPort) {
        this.displayPort = Objects.requireNonNull(displayPort, "displayPort");
    }

    public void updateDisplay(Integer userId, Integer id, Integer isDisplayed) {
        displayPort.updateDisplay(userId, id, isDisplayed);
    }
}

