package edu.cuit.bc.messaging.application.usecase;

import edu.cuit.bc.messaging.application.port.MessageQueryPort;
import edu.cuit.domain.entity.MsgEntity;

import java.util.List;
import java.util.Objects;

/**
 * 查询消息用例（读模型入口）。
 *
 * <p>保持行为不变：用例仅做编排与依赖隔离，查询与组装逻辑在端口适配器中原样搬运。</p>
 */
public class QueryMessageUseCase {
    private final MessageQueryPort queryPort;

    public QueryMessageUseCase(MessageQueryPort queryPort) {
        this.queryPort = Objects.requireNonNull(queryPort, "queryPort");
    }

    public List<MsgEntity> queryMsg(Integer userId, Integer type, Integer mode) {
        return queryPort.queryMsg(userId, type, mode);
    }

    public List<MsgEntity> queryTargetAmountMsg(Integer userId, Integer num, Integer type) {
        return queryPort.queryTargetAmountMsg(userId, num, type);
    }
}

