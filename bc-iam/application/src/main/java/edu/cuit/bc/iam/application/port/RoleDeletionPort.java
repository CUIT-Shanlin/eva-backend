package edu.cuit.bc.iam.application.port;

/**
 * 角色删除端口（写侧持久化/外部依赖）。
 *
 * <p>保持行为不变：校验、DB 删除、缓存失效与日志顺序由端口适配器原样搬运旧实现。</p>
 */
public interface RoleDeletionPort {

    /**
     * 删除角色（沿用旧 gateway 语义）。
     *
     * @param roleId 角色ID
     */
    void deleteRole(Integer roleId);
}

