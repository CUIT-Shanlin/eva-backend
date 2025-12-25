package edu.cuit.bc.iam.application.usecase;

import edu.cuit.bc.iam.application.port.RoleDeletionPort;

/**
 * 角色删除用例（写模型入口）。
 *
 * <p>说明：现阶段仅做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class DeleteRoleUseCase {
    private final RoleDeletionPort deletionPort;

    public DeleteRoleUseCase(RoleDeletionPort deletionPort) {
        this.deletionPort = deletionPort;
    }

    public void execute(Integer roleId) {
        deletionPort.deleteRole(roleId);
    }
}

