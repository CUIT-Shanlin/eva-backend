package edu.cuit.bc.iam.application.usecase;

import edu.cuit.bc.iam.application.port.UserEntityQueryPort;
import edu.cuit.domain.entity.user.biz.UserEntity;

import java.util.Optional;

/**
 * 按用户名查询用户实体用例（读模型入口）。
 *
 * <p>说明：现阶段仅做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class FindUserByUsernameUseCase {
    private final UserEntityQueryPort queryPort;

    public FindUserByUsernameUseCase(UserEntityQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public Optional<UserEntity> execute(String username) {
        return queryPort.findByUsername(username);
    }
}

