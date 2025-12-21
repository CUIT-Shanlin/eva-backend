package edu.cuit.bc.iam.application.usecase;

import edu.cuit.bc.iam.application.port.UserBasicQueryPort;
import java.util.Optional;

/**
 * 按用户ID查询用户名用例（读模型入口）。
 *
 * <p>说明：现阶段仅做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class FindUsernameByIdUseCase {
    private final UserBasicQueryPort queryPort;

    public FindUsernameByIdUseCase(UserBasicQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public Optional<String> execute(Integer id) {
        return queryPort.findUsernameById(id);
    }
}

