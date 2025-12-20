package edu.cuit.bc.iam.application.usecase;

import edu.cuit.bc.iam.application.port.UserStatusUpdatePort;

/**
 * 更新用户状态用例（写模型入口）。
 *
 * <p>说明：现阶段仅做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class UpdateUserStatusUseCase {
    private final UserStatusUpdatePort statusUpdatePort;

    public UpdateUserStatusUseCase(UserStatusUpdatePort statusUpdatePort) {
        this.statusUpdatePort = statusUpdatePort;
    }

    public void execute(Integer userId, Integer status) {
        statusUpdatePort.updateStatus(userId, status);
    }
}

