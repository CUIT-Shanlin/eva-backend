package edu.cuit.app.security;

import cn.dev33.satoken.stp.StpInterface;
import edu.cuit.domain.entity.user.biz.MenuEntity;
import edu.cuit.domain.entity.user.biz.RoleEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 权限加载
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final UserQueryGateway userQueryGateway;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        if ("admin".equals(loginId)) return List.of("*");
        Optional<UserEntity> user = userQueryGateway.findByUsername((String) loginId);
        return user.map(userEntity -> {
            List<RoleEntity> roles = userEntity.getRoles();
            List<String> menus = new ArrayList<>();
            for (RoleEntity role : roles) {
                if (role.getStatus() == 0) continue;
                menus.addAll(role.getMenus().stream()
                        .filter(menuEntity -> menuEntity.getType() == 2)
                        .filter(menuEntity -> menuEntity.getStatus() == 1)
                        .map(MenuEntity::getPerms).toList());
            }
            return menus;
        }).orElse(new ArrayList<>());
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if ("admin".equals(loginId)) return List.of("*");
        Optional<UserEntity> user = userQueryGateway.findByUsername((String) loginId);
        return user.map(userEntity ->
                userEntity.getRoles().stream()
                        .filter(roleEntity -> roleEntity.getStatus() == 1)
                        .map(RoleEntity::getRoleName).toList())
                .orElse(new ArrayList<>());
    }
}
