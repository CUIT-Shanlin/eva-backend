package edu.cuit.bc.iam.application.usecase;

import edu.cuit.bc.iam.application.port.UserBasicQueryPort;
import java.util.Optional;

/**
 * 查询用户状态用例（读模型入口）。
 *
 * <p>说明：现阶段仅做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class GetUserStatusUseCase {
    private final UserBasicQueryPort queryPort;

    public GetUserStatusUseCase(UserBasicQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public Optional<Integer> execute(Integer id) {
        return queryPort.getUserStatus(id);
    }
}

