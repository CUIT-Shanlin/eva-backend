package edu.cuit.bc.iam.application.port;

import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;

import java.util.Optional;

/**
 * 用户实体查询端口（读侧持久化/外部依赖）。
 *
 * <p>保持行为不变：查询条件、排序、映射规则（含角色/菜单装配）、异常类型/异常文案等逻辑由端口适配器原样搬运旧实现。</p>
 *
 * <p>过渡期为收敛编译期依赖面：返回类型使用 {@code Optional<?>} 与 {@code PaginationResultEntity<?>}，
 * 避免在 application 模块中直接暴露旧领域实体类型导致 Maven 循环依赖；当前实际返回值仍为
 * {@code edu.cuit.domain.entity.user.biz.UserEntity}，由端口适配器保持旧语义。</p>
 */
public interface UserEntityQueryPort {

    /**
     * 按用户ID查询用户实体（沿用旧 gateway 语义）。
     */
    Optional<?> findById(Integer id);

    /**
     * 按用户名查询用户实体（沿用旧 gateway 语义）。
     */
    Optional<?> findByUsername(String username);

    /**
     * 分页查询用户实体（沿用旧 gateway 语义）。
     */
    PaginationResultEntity<?> page(PagingQuery<GenericConditionalQuery> query);
}
