package edu.cuit.bc.messaging.application.port;

import edu.cuit.client.dto.data.msg.GenericRequestMsg;

/**
 * 消息新增端口（写侧持久化/外部依赖）。
 *
 * <p>保持行为不变：消息落库与 id/createTime 补齐策略由端口适配器原样搬运旧实现。</p>
 */
public interface MessageInsertionPort {

    /**
     * 插入消息（沿用旧 gateway 签名与行为）。
     *
     * @param msg 请求消息
     */
    void insertMessage(GenericRequestMsg msg);
}

