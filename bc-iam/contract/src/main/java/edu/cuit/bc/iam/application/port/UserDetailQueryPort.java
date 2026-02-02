package edu.cuit.bc.iam.application.port;

import edu.cuit.bc.iam.application.contract.dto.clientobject.user.UserDetailCO;

import java.util.Optional;

/**
 * 用户详情查询端口（读侧持久化/缓存/外部依赖）。
 *
 * <p>保持行为不变：由端口适配器承接历史实现（含缓存命中/回源顺序、异常与空值语义）。</p>
 *
 * <p>说明：该端口返回 contract DTO（而非领域实体），用于避免 contract 反向依赖领域模块引入 Maven 循环依赖。</p>
 */
public interface UserDetailQueryPort {

    /**
     * 按用户ID查询用户详情（沿用旧实现语义）。
     */
    Optional<UserDetailCO> findById(Integer id);
}

