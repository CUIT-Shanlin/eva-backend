package edu.cuit.bc.iam.application.port;

import java.util.Optional;

/**
 * 用户基础信息查询端口（读侧持久化/缓存/外部依赖）。
 *
 * <p>保持行为不变：缓存命中/回源顺序与 key 规则、历史 Optional/null/NPE 表现等，均由端口适配器原样搬运旧实现。</p>
 */
public interface UserBasicQueryPort {

    /**
     * 按用户名查询用户ID（沿用旧 gateway 语义）。
     */
    Optional<Integer> findIdByUsername(String username);

    /**
     * 按用户ID查询用户名（沿用旧 gateway 语义）。
     */
    Optional<String> findUsernameById(Integer id);

    /**
     * 查询用户状态（沿用旧 gateway 语义，含历史空值/NPE 表现）。
     */
    Optional<Integer> getUserStatus(Integer id);

    /**
     * 校验用户名是否存在（沿用旧 gateway 语义，含缓存命中判定）。
     */
    Boolean isUsernameExist(String username);
}

