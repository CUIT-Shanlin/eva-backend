package edu.cuit.bc.iam.application.port;

import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;

import java.util.List;

/**
 * 用户目录/分页查询端口（过渡）。
 *
 * <p>目的：让依赖方在编译期只依赖 {@code bc-iam-contract}，逐步去除对旧 {@code UserQueryGateway} 的直接依赖，
 * 同时保持行为不变（分页查询、全量用户名、全量用户列表等能力沿用旧实现语义）。</p>
 *
 * <p>注意：返回类型使用 {@code PaginationResultEntity<?>}，避免在 contract 中直接暴露旧领域实体类型导致 Maven 循环依赖；
 * 过渡期 records 实际元素为 {@code edu.cuit.domain.entity.user.biz.UserEntity}。</p>
 */
public interface UserDirectoryPageQueryPort {

    /**
     * 分页获取用户信息（沿用旧 gateway 语义）。
     */
    PaginationResultEntity<?> page(PagingQuery<GenericConditionalQuery> query);

    /**
     * 获取所有用户（极简响应模型，沿用旧 gateway 语义）。
     */
    List<SimpleResultCO> allUser();

    /**
     * 获取所有用户名（沿用旧 gateway 语义）。
     */
    List<String> findAllUsername();
}
