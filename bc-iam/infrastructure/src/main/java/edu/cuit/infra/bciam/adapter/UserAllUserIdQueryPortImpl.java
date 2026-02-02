package edu.cuit.infra.bciam.adapter;

import edu.cuit.bc.iam.application.port.UserAllUserIdQueryPort;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * bc-iam：用户ID列表查询端口适配器（保持历史行为不变）。
 *
 * <p>保持缓存/切面触发点不变：内部委托旧 {@link UserQueryGateway#findAllUserId()}（其仍承载缓存注解）。</p>
 */
@Component
@RequiredArgsConstructor
public class UserAllUserIdQueryPortImpl implements UserAllUserIdQueryPort {

    private final UserQueryGateway userQueryGateway;

    @Override
    public List<Integer> findAllUserId() {
        return userQueryGateway.findAllUserId();
    }
}

