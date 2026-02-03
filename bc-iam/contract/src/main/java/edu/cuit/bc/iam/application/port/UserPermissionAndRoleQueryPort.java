package edu.cuit.bc.iam.application.port;

import java.util.List;

/**
 * 用户权限与角色查询端口（供安全/鉴权适配层使用，保持行为不变）。
 *
 * <p>约束：实现方必须内部委托旧 gateway 的 {@code findByUsername}，以保持缓存/切面触发点不变。</p>
 */
public interface UserPermissionAndRoleQueryPort {

    List<String> findPermissionListByUsername(String username);

    List<String> findRoleListByUsername(String username);
}

