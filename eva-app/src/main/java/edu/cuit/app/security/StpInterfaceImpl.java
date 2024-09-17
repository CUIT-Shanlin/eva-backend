package edu.cuit.app.security;

import cn.dev33.satoken.stp.StpInterface;
import edu.cuit.domain.gateway.user.LdapPersonGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 权限加载
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final LdapPersonGateway ldapPersonGateway;

    //TODO 实现权限接口
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return List.of();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return List.of();
    }
}
