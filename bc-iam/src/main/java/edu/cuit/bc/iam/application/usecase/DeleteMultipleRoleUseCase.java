package edu.cuit.bc.iam.application.usecase;

import edu.cuit.bc.iam.application.port.RoleBatchDeletionPort;
import java.util.List;

/**
 * 角色批量删除用例（写模型入口）。
 *
 * <p>说明：现阶段仅做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class DeleteMultipleRoleUseCase {
    private final RoleBatchDeletionPort deletionPort;

    public DeleteMultipleRoleUseCase(RoleBatchDeletionPort deletionPort) {
        this.deletionPort = deletionPort;
    }

    public void execute(List<Integer> roleIds) {
        deletionPort.deleteMultipleRole(roleIds);
    }
}

