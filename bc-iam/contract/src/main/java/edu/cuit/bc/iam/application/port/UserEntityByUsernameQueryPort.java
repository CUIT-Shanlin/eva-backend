package edu.cuit.bc.iam.application.port;

import java.util.Optional;

/**
 * 用户实体按用户名查询端口（读侧持久化/缓存/外部依赖）。
 *
 * <p>保持行为不变：端口适配器内部委托旧 gateway（其仍承载缓存注解/切面触发点）。</p>
 *
 * <p>注意：返回类型使用 {@code Optional<?>}，避免在 contract 中直接暴露旧领域实体类型导致 Maven 循环依赖；
 * 具体返回值在过渡期为 {@code edu.cuit.domain.entity.user.biz.UserEntity}。</p>
 */
public interface UserEntityByUsernameQueryPort {

    /**
     * 按用户名查询用户实体（沿用旧 gateway 语义）。
     */
    Optional<?> findByUsername(String username);
}

