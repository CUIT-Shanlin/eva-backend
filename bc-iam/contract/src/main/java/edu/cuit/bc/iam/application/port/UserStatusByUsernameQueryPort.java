package edu.cuit.bc.iam.application.port;

import java.util.Optional;

/**
 * 用户状态按用户名查询端口（保持历史行为不变）。
 *
 * <p>约束：实现方必须内部委托旧 gateway 的 {@code findByUsername}，以保持缓存/切面触发点与调用次数不变。</p>
 */
public interface UserStatusByUsernameQueryPort {

    /**
     * 按用户名查询用户状态（沿用旧 gateway 语义）。
     */
    Optional<Integer> findStatusByUsername(String username);
}

