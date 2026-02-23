package edu.cuit.bc.iam.application.port;

import edu.cuit.bc.iam.application.contract.dto.clientobject.user.UserDetailCO;

import java.util.Optional;

/**
 * 用户详情直查端口（按用户名，不走缓存/切面副作用）。
 *
 * <p>保持行为不变：该端口用于替代其它 BC 内部对 {@code sysUserMapper.selectOne(eq username)} 的跨 BC 直连写法，
 * 实现方应沿用旧实现语义（查询条件、异常与空值语义保持不变），且不应引入新的缓存命中/回源副作用。</p>
 *
 * <p>说明：调用方当前仅依赖 {@link UserDetailCO#getId()} 与 {@link UserDetailCO#getName()}，
 * 其余字段可为空（过渡期按最小需要填充）。</p>
 */
public interface UserDetailByUsernameDirectQueryPort {

    /**
     * 按用户名直查用户详情（沿用旧实现语义）。
     */
    Optional<UserDetailCO> findByUsername(String username);
}

