package edu.cuit.bc.iam.application.usecase;

import edu.cuit.bc.iam.application.port.MenuInfoUpdatePort;
import edu.cuit.client.dto.cmd.user.UpdateMenuCmd;

/**
 * 菜单信息更新用例（写模型入口）。
 *
 * <p>说明：现阶段仅做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class UpdateMenuInfoUseCase {
    private final MenuInfoUpdatePort updatePort;

    public UpdateMenuInfoUseCase(MenuInfoUpdatePort updatePort) {
        this.updatePort = updatePort;
    }

    public void execute(UpdateMenuCmd cmd) {
        updatePort.updateMenuInfo(cmd);
    }
}

