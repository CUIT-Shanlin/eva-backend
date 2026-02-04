package edu.cuit.infra.bciam.adapter;

import edu.cuit.bc.iam.application.port.UserAllUserIdAndEntityByIdQueryPort;
import edu.cuit.infra.gateway.user.UserQueryCacheGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * bc-iam：用户ID列表查询端口适配器（保持历史行为不变）。
 *
 * <p>保持缓存/切面触发点不变：内部委托 {@link UserQueryCacheGateway#findAllUserId()}，
 * 其实现仍为旧 {@code UserQueryGatewayImpl} 承载 {@code @LocalCached}。</p>
 */
@Component
@RequiredArgsConstructor
public class UserAllUserIdQueryPortImpl implements UserAllUserIdAndEntityByIdQueryPort {

    private final UserQueryCacheGateway userQueryCacheGateway;

    @Override
    public List<Integer> findAllUserId() {
        return userQueryCacheGateway.findAllUserId();
    }

    @Override
    public Optional<?> findById(Integer id) {
        return userQueryCacheGateway.findById(id);
    }
}
