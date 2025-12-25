package edu.cuit.bc.iam.application.port;

import java.util.List;

/**
 * 角色权限分配端口（写侧出站依赖）。
 *
 * <p>说明：现阶段仅做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public interface RolePermissionAssignmentPort {
    void assignPerms(Integer roleId, List<Integer> menuIds);
}

