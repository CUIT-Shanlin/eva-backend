package edu.cuit.bc.iam.application.port;

/**
 * 菜单删除端口（写侧持久化/外部依赖）。
 *
 * <p>保持行为不变：递归删除顺序、根节点缓存失效触发两次等历史行为由端口适配器原样搬运旧实现。</p>
 */
public interface MenuDeletionPort {

    /**
     * 删除单个菜单（沿用旧 gateway 语义）。
     *
     * @param menuId 菜单 id
     */
    void deleteMenu(Integer menuId);
}

