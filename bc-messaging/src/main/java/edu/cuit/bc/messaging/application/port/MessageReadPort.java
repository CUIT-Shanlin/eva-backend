package edu.cuit.bc.messaging.application.port;

/**
 * 消息已读状态端口（写侧持久化/外部依赖）。
 */
public interface MessageReadPort {

    /**
     * 更新单条消息“已读”状态。
     *
     * @param userId 接收者 ID（用于校验“只能修改自己的消息”）
     * @param id     消息 ID
     * @param isRead 已读标记（沿用历史入参：0/1）
     */
    void updateRead(Integer userId, Integer id, Integer isRead);

    /**
     * 批量更新某模式下的消息为“已读”。
     *
     * @param userId 接收者 ID
     * @param mode   消息模式
     */
    void markAllRead(Integer userId, Integer mode);
}

