package edu.cuit.bc.iam.application.port;

import java.util.List;

/**
 * 菜单批量删除端口（写侧持久化/外部依赖）。
 *
 * <p>保持行为不变：递归删除顺序、缓存失效次数/时机、异常文案与日志顺序由端口适配器原样搬运旧实现。</p>
 */
public interface MenuBatchDeletionPort {

    /**
     * 批量删除菜单（沿用旧 gateway 语义）。
     *
     * @param menuIds 菜单 id 列表
     */
    void deleteMultipleMenu(List<Integer> menuIds);
}

