package edu.cuit.bc.iam.application.usecase;

import edu.cuit.bc.iam.application.port.UserMenuCacheInvalidationPort;

/**
 * 菜单变更触发的用户菜单缓存失效用例（写模型入口）。
 *
 * <p>说明：现阶段仅做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class HandleUserMenuCacheUseCase {
    private final UserMenuCacheInvalidationPort invalidationPort;

    public HandleUserMenuCacheUseCase(UserMenuCacheInvalidationPort invalidationPort) {
        this.invalidationPort = invalidationPort;
    }

    public void execute(Integer menuId) {
        invalidationPort.handleUserMenuCache(menuId);
    }
}

