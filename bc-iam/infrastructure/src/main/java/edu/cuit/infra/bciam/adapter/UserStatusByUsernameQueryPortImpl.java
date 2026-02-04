package edu.cuit.infra.bciam.adapter;

import edu.cuit.bc.iam.application.port.UserStatusByUsernameQueryPort;
import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * bc-iam：用户状态按用户名查询端口适配器（保持历史行为不变）。
 *
 * <p>保持缓存/切面触发点不变：内部委托旧 {@link UserQueryGateway#findByUsername(String)}（其仍承载缓存注解）。</p>
 */
@Component
@RequiredArgsConstructor
public class UserStatusByUsernameQueryPortImpl implements UserStatusByUsernameQueryPort {

    private final UserQueryGateway userQueryGateway;

    @Override
    public Optional<Integer> findStatusByUsername(String username) {
        return userQueryGateway.findByUsername(username)
                .map(userEntity -> ((UserEntity) userEntity).getStatus());
    }
}

