package edu.cuit.bc.iam.application.port;

/**
 * 用户状态更新端口（写侧持久化/外部依赖）。
 *
 * <p>保持行为不变：校验、更新顺序、缓存失效、日志等逻辑由端口适配器原样搬运旧实现。</p>
 */
public interface UserStatusUpdatePort {

    /**
     * 更新用户状态（沿用旧 gateway 语义）。
     *
     * @param userId 用户ID
     * @param status 状态（0：禁用，1：正常）
     */
    void updateStatus(Integer userId, Integer status);
}

