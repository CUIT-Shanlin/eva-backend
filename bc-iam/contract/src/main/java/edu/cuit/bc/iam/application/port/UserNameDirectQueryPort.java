package edu.cuit.bc.iam.application.port;

/**
 * 用户姓名直查端口（不走缓存/切面副作用）。
 *
 * <p>保持行为不变：该端口用于替代其它 BC 内部对 {@code sysUserMapper.selectById(id).getName()} 的跨 BC 直连写法，
 * 实现方应沿用旧实现语义（查询条件、异常与空值语义保持不变），且不应引入新的缓存命中/回源副作用。</p>
 *
 * <p>说明：返回值允许为 {@code null}（沿用旧实现中 {@code getName()} 可能返回 null 的语义）。</p>
 */
public interface UserNameDirectQueryPort {

    /**
     * 按用户ID直查姓名（沿用旧实现语义）。
     */
    String findNameById(Integer id);
}

