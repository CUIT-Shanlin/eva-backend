package edu.cuit.bc.iam.application.usecase;

import edu.cuit.bc.iam.application.port.RoleInfoUpdatePort;
import edu.cuit.client.dto.cmd.user.UpdateRoleCmd;

/**
 * 角色信息更新用例（写模型入口）。
 *
 * <p>说明：现阶段仅做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class UpdateRoleInfoUseCase {
    private final RoleInfoUpdatePort updatePort;

    public UpdateRoleInfoUseCase(RoleInfoUpdatePort updatePort) {
        this.updatePort = updatePort;
    }

    public void execute(UpdateRoleCmd cmd) {
        updatePort.updateRoleInfo(cmd);
    }
}

