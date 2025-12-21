package edu.cuit.bc.iam.application.port;

/**
 * 菜单变更触发的用户菜单缓存失效端口（写侧出站依赖）。
 *
 * <p>说明：现阶段仅做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public interface UserMenuCacheInvalidationPort {
    void handleUserMenuCache(Integer menuId);
}

