package edu.cuit.infra.bciam.adapter;

import edu.cuit.bc.iam.application.port.UserPermissionAndRoleQueryPort;
import edu.cuit.bc.iam.application.port.UserEntityByUsernameQueryPort;
import edu.cuit.bc.iam.application.port.UserStatusByUsernameQueryPort;
import edu.cuit.domain.entity.user.biz.MenuEntity;
import edu.cuit.domain.entity.user.biz.RoleEntity;
import edu.cuit.infra.convertor.user.UserConverter;
import edu.cuit.infra.gateway.user.UserQueryCacheGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * bc-iam：用户实体按用户名查询端口适配器（保持历史行为不变）。
 *
 * <p>保持缓存/切面触发点不变：内部委托 {@link UserQueryCacheGateway#findByUsername(String)}，
 * 其实现仍为旧 {@code UserQueryGatewayImpl} 承载 {@code @LocalCached}。</p>
 */
@Component
@RequiredArgsConstructor
public class UserEntityByUsernameQueryPortImpl implements UserEntityByUsernameQueryPort, UserPermissionAndRoleQueryPort, UserStatusByUsernameQueryPort {

    private final UserQueryCacheGateway userQueryCacheGateway;
    private final UserConverter userConverter;

    @Override
    public Optional<?> findByUsername(String username) {
        return userQueryCacheGateway.findByUsername(username);
    }

    @Override
    public Optional<Integer> findStatusByUsername(String username) {
        return userQueryCacheGateway.findByUsername(username)
                // 用 +0 触发拆箱，保持历史空值/NPE 表现不变
                .map(userEntity -> userConverter.statusOf(userEntity, true) + 0);
    }

    @Override
    public List<String> findPermissionListByUsername(String username) {
        Optional<?> user = userQueryCacheGateway.findByUsername(username);
        return user.map(userEntity -> {
            List<RoleEntity> roles = userConverter.rolesOf(userEntity, true);
            List<String> menus = new ArrayList<>();
            for (RoleEntity role : roles) {
                if (role.getStatus() == 0) continue;
                menus.addAll(role.getMenus().stream()
                        .filter(menuEntity -> menuEntity.getType() == 2 && menuEntity.getPerms() != null)
                        .filter(menuEntity -> menuEntity.getStatus() == 1)
                        .map(MenuEntity::getPerms).toList());
            }
            return menus;
        }).orElse(new ArrayList<>());
    }

    @Override
    public List<String> findRoleListByUsername(String username) {
        Optional<?> user = userQueryCacheGateway.findByUsername(username);
        return user.map(userEntity ->
                        userConverter.rolesOf(userEntity, true).stream()
                                .filter(roleEntity -> roleEntity.getStatus() == 1)
                                .map(RoleEntity::getRoleName).toList())
                .orElse(new ArrayList<>());
    }
}
