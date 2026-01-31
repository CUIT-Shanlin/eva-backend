package edu.cuit.infra.bciam.adapter;

import edu.cuit.bc.iam.application.port.UserNameQueryPort;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * bc-iam：用户姓名查询端口适配器（保持历史行为不变）。
 *
 * <p>保持缓存/切面触发点不变：内部委托旧 {@link UserQueryGateway#findById(Integer)}（其仍承载缓存注解）。</p>
 */
@Component
@RequiredArgsConstructor
public class UserNameQueryPortImpl implements UserNameQueryPort {
    private final UserQueryGateway userQueryGateway;

    @Override
    public Optional<String> findNameById(Integer id) {
        return userQueryGateway.findById(id).map(user -> user.getName());
    }
}
