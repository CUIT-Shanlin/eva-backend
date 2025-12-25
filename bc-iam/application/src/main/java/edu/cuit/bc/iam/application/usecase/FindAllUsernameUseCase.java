package edu.cuit.bc.iam.application.usecase;

import edu.cuit.bc.iam.application.port.UserDirectoryQueryPort;
import java.util.List;

/**
 * 查询所有用户名用例（读模型入口）。
 *
 * <p>说明：现阶段仅做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class FindAllUsernameUseCase {
    private final UserDirectoryQueryPort queryPort;

    public FindAllUsernameUseCase(UserDirectoryQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public List<String> execute() {
        return queryPort.findAllUsername();
    }
}

