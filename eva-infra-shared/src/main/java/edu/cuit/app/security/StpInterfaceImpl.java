package edu.cuit.app.security;

import cn.dev33.satoken.stp.StpInterface;
import edu.cuit.bc.iam.application.port.UserPermissionAndRoleQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 权限加载
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final UserPermissionAndRoleQueryPort userPermissionAndRoleQueryPort;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        if ("admin".equals(loginId)) return List.of("*");
        return userPermissionAndRoleQueryPort.findPermissionListByUsername((String) loginId);
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        if ("admin".equals(loginId)) return List.of("*");
        return userPermissionAndRoleQueryPort.findRoleListByUsername((String) loginId);
    }
}
