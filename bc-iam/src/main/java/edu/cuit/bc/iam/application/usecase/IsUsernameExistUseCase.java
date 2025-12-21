package edu.cuit.bc.iam.application.usecase;

import edu.cuit.bc.iam.application.port.UserBasicQueryPort;

/**
 * 校验用户名是否存在用例（读模型入口）。
 *
 * <p>说明：现阶段仅做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class IsUsernameExistUseCase {
    private final UserBasicQueryPort queryPort;

    public IsUsernameExistUseCase(UserBasicQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public Boolean execute(String username) {
        return queryPort.isUsernameExist(username);
    }
}

