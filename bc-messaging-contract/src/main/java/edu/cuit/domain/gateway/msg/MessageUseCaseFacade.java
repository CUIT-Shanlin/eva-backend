package edu.cuit.domain.gateway.msg;

import edu.cuit.client.dto.data.msg.GenericRequestMsg;
import edu.cuit.domain.entity.MsgEntity;
import java.util.List;

/**
 * 消息域用例门面（仅用于“保持行为不变”的渐进式重构阶段）。
 *
 * <p>背景：旧系统中 {@code eva-infra} 的 {@code MsgGatewayImpl} 直接依赖消息域用例实现，导致编译期依赖面偏大。
 * 该门面用于把旧 gateway 对消息域的编译期依赖收敛为仅依赖 {@code eva-domain} 的接口，
 * 具体实现仍由 {@code bc-messaging} 在运行时提供，确保行为不变。</p>
 */
public interface MessageUseCaseFacade {

    List<MsgEntity> queryMsg(Integer userId, Integer type, Integer mode);

    List<MsgEntity> queryTargetAmountMsg(Integer userId, Integer num, Integer type);

    void updateDisplay(Integer userId, Integer id, Integer isDisplayed);

    void updateRead(Integer userId, Integer id, Integer isRead);

    void markAllRead(Integer userId, Integer mode);

    void insertMessage(GenericRequestMsg msg);

    void deleteByTask(Integer taskId, Integer type);

    void deleteUserMessages(Integer userId, Integer mode, Integer type);
}

