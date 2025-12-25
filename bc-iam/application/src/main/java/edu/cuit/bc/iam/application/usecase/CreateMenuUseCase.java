package edu.cuit.bc.iam.application.usecase;

import edu.cuit.bc.iam.application.port.MenuCreationPort;
import edu.cuit.bc.iam.application.contract.dto.cmd.user.NewMenuCmd;

/**
 * 菜单创建用例（写模型入口）。
 *
 * <p>说明：现阶段仅做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class CreateMenuUseCase {
    private final MenuCreationPort creationPort;

    public CreateMenuUseCase(MenuCreationPort creationPort) {
        this.creationPort = creationPort;
    }

    public void execute(NewMenuCmd cmd) {
        creationPort.createMenu(cmd);
    }
}

