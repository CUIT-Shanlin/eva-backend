package edu.cuit.bc.iam.application.usecase;

import edu.cuit.bc.iam.application.port.RoleCreationPort;
import edu.cuit.bc.iam.application.contract.dto.cmd.user.NewRoleCmd;

/**
 * 角色创建用例（写模型入口）。
 *
 * <p>说明：现阶段仅做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class CreateRoleUseCase {
    private final RoleCreationPort creationPort;

    public CreateRoleUseCase(RoleCreationPort creationPort) {
        this.creationPort = creationPort;
    }

    public void execute(NewRoleCmd cmd) {
        creationPort.createRole(cmd);
    }
}

