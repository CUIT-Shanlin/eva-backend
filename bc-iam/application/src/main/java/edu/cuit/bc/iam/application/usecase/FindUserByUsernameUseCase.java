package edu.cuit.bc.iam.application.usecase;

import edu.cuit.bc.iam.application.port.UserEntityByUsernameQueryPort;
import edu.cuit.bc.iam.application.port.UserEntityQueryPort;

import java.util.Optional;

/**
 * 按用户名查询用户实体用例（读模型入口）。
 *
 * <p>说明：现阶段仅做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class FindUserByUsernameUseCase {
    private final UserEntityByUsernameQueryPort queryPort;

    public FindUserByUsernameUseCase(UserEntityByUsernameQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    /**
     * 过渡期保持旧语义：此构造仅用于复用现有 wiring/测试（不在用例内暴露旧领域实体类型）。
     */
    public FindUserByUsernameUseCase(UserEntityQueryPort legacyQueryPort) {
        this.queryPort = legacyQueryPort::findByUsername;
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> execute(String username) {
        return (Optional<T>) queryPort.findByUsername(username);
    }
}
