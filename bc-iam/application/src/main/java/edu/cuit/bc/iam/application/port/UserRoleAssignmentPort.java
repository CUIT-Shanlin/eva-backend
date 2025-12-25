package edu.cuit.bc.iam.application.port;

import java.util.List;

/**
 * 用户角色分配端口（写侧持久化/外部依赖）。
 *
 * <p>保持行为不变：校验、删除/插入顺序、缓存与日志由端口适配器原样搬运旧实现。</p>
 */
public interface UserRoleAssignmentPort {

    /**
     * 为用户分配角色（沿用旧 gateway 语义）。
     *
     * @param userId 用户 ID
     * @param roleId 角色 ID 列表
     */
    void assignRole(Integer userId, List<Integer> roleId);
}

