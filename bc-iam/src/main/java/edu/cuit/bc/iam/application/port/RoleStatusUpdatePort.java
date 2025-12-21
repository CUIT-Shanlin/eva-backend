package edu.cuit.bc.iam.application.port;

/**
 * 角色状态更新端口（写侧持久化/外部依赖）。
 *
 * <p>保持行为不变：校验、DB 写入、缓存失效与日志顺序由端口适配器原样搬运旧实现。</p>
 */
public interface RoleStatusUpdatePort {

    /**
     * 更新角色状态（沿用旧 gateway 语义）。
     *
     * @param roleId 角色ID
     * @param status 状态
     */
    void updateRoleStatus(Integer roleId, Integer status);
}

