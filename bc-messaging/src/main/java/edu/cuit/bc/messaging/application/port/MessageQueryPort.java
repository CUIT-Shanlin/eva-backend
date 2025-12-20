package edu.cuit.bc.messaging.application.port;

import edu.cuit.domain.entity.MsgEntity;

import java.util.List;

/**
 * 消息查询端口（读侧外部依赖）。
 *
 * <p>保持行为不变：过滤规则、排序规则、异常类型与异常文案由端口适配器原样搬运旧实现。</p>
 */
public interface MessageQueryPort {

    /**
     * 查询消息列表。
     *
     * @param userId 接收者 ID
     * @param type   消息类型；若为 null 或 <0 则不作为筛选条件
     * @param mode   消息模式；若为 null 或 <0 则不作为筛选条件
     */
    List<MsgEntity> queryMsg(Integer userId, Integer type, Integer mode);

    /**
     * 查询指定数量的消息列表。
     *
     * @param userId 接收者 ID
     * @param num    指定消息数目；若为 null 或 <0 则不作为 limit 条件
     * @param type   消息类型；若为 null 或 <0 则不作为筛选条件
     */
    List<MsgEntity> queryTargetAmountMsg(Integer userId, Integer num, Integer type);
}

