package edu.cuit.bc.iam.application.usecase;

import edu.cuit.bc.iam.application.port.MenuBatchDeletionPort;

import java.util.List;

/**
 * 菜单批量删除用例（写模型入口）。
 *
 * <p>说明：现阶段仅做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class DeleteMultipleMenuUseCase {
    private final MenuBatchDeletionPort deletionPort;

    public DeleteMultipleMenuUseCase(MenuBatchDeletionPort deletionPort) {
        this.deletionPort = deletionPort;
    }

    public void execute(List<Integer> menuIds) {
        deletionPort.deleteMultipleMenu(menuIds);
    }
}

