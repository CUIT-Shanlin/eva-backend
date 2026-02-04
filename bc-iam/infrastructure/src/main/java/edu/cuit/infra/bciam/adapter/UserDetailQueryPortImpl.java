package edu.cuit.infra.bciam.adapter;

import edu.cuit.bc.iam.application.contract.dto.clientobject.user.UserDetailCO;
import edu.cuit.bc.iam.application.port.UserDetailQueryPort;
import edu.cuit.infra.gateway.user.UserQueryCacheGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * bc-iam：用户详情查询端口适配器（保持历史行为不变）。
 *
 * <p>保持缓存/切面触发点不变：内部委托 {@link UserQueryCacheGateway#findById(Integer)}，
 * 其实现仍为旧 {@code UserQueryGatewayImpl} 承载 {@code @LocalCached}。</p>
 */
@Component
@RequiredArgsConstructor
public class UserDetailQueryPortImpl implements UserDetailQueryPort {

    private final UserQueryCacheGateway userQueryCacheGateway;

    @Override
    public Optional<UserDetailCO> findById(Integer id) {
        return userQueryCacheGateway.findById(id).map(userObject -> {
            edu.cuit.domain.entity.user.biz.UserEntity user = (edu.cuit.domain.entity.user.biz.UserEntity) userObject;
            return new UserDetailCO()
                    .setId(user.getId() == null ? null : user.getId().longValue())
                    .setUsername(user.getUsername())
                    .setName(user.getName());
        });
    }
}
