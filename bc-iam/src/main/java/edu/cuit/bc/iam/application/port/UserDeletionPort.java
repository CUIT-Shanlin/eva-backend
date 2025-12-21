package edu.cuit.bc.iam.application.port;

/**
 * 用户删除端口（写侧持久化/外部依赖）。
 *
 * <p>保持行为不变：校验、删除顺序（DB → LDAP → 角色解绑 → 缓存失效 → 日志）、异常类型/文案等逻辑由端口适配器原样搬运旧实现。</p>
 */
public interface UserDeletionPort {

    /**
     * 删除用户（沿用旧 gateway 语义）。
     *
     * @param userId 用户ID
     */
    void deleteUser(Integer userId);
}

