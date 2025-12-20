package edu.cuit.bc.messaging.application.port;

/**
 * 消息显示状态端口（写侧持久化/外部依赖）。
 *
 * <p>保持行为不变：权限校验与异常文案由端口适配器原样搬运旧实现。</p>
 */
public interface MessageDisplayPort {

    /**
     * 更新单条消息“是否已展示”状态。
     *
     * @param userId      接收者 ID（用于校验“只能修改自己的消息”）
     * @param id          消息 ID
     * @param isDisplayed 展示标记（沿用历史入参：0/1）
     */
    void updateDisplay(Integer userId, Integer id, Integer isDisplayed);
}

