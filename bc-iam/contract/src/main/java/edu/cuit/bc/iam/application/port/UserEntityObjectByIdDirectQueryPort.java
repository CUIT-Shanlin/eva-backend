package edu.cuit.bc.iam.application.port;

/**
 * 用户实体对象（含角色信息）按 ID 查询端口（供其它 BC 基础设施层使用，保持行为不变）。
 *
 * <p>约束：该端口用于替代“跨 BC 直连 IAM 表（sys_user/sys_user_role/sys_role）”的用法。</p>
 *
 * <p><b>保持行为不变（重要）</b>：实现方应沿用调用方的旧实现语义（SQL、空值/异常语义与副作用顺序不变），
 * 且不应引入新的缓存命中/回源副作用（例如不应改为委托带 {@code @LocalCached} 的旧 gateway）。</p>
 *
 * <p>说明：返回类型使用 {@code Object}，用于避免在 contract 中暴露旧领域实体类型导致 Maven 循环依赖；
 * 过渡期实际返回值为旧实现构造的“用户实体对象”（调用方按原方式透传即可）。</p>
 */
public interface UserEntityObjectByIdDirectQueryPort {

    /**
     * 按用户ID查询用户实体对象（含角色信息，沿用旧实现语义）。
     */
    Object findById(Integer id);
}

