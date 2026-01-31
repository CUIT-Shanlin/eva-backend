package edu.cuit.bc.iam.application.port;

import java.util.Optional;

/**
 * 用户姓名查询端口（读侧持久化/缓存/外部依赖）。
 *
 * <p>保持行为不变：缓存命中/回源顺序、异常与空值语义等均由端口适配器承接历史实现。</p>
 */
public interface UserNameQueryPort {

    /**
     * 按用户ID查询姓名（沿用旧实现语义）。
     */
    Optional<String> findNameById(Integer id);
}

