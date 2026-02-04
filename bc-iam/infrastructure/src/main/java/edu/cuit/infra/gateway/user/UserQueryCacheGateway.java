package edu.cuit.infra.gateway.user;

import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import java.util.List;
import java.util.Optional;

/**
 * <p>过渡期：用户查询“缓存触发点”内部网关。</p>
 *
 * <p>背景：历史上旧 {@code UserQueryGatewayImpl} 通过 {@code @LocalCached} 承载缓存/切面触发点；
 * 但其上游接口（以及返回类型）包含旧领域实体 {@code UserEntity}，使得“依赖面收敛（去编译期依赖）”难以按单类闭环推进。</p>
 *
 * <p>本接口的目的：允许基础设施侧端口适配器逐步从编译期依赖旧 {@code UserQueryGateway}（eva-domain）
 * 收敛为依赖本接口（bc-iam/infrastructure 内部），同时仍保证调用最终进入旧 {@code UserQueryGatewayImpl}
 * 以触发历史缓存/切面入口（行为不变）。</p>
 *
 * <p>注意：这是 <b>bc-iam/infrastructure 内部接口</b>，不对外作为契约；任何实现必须保持缓存 key/area 与调用顺序不变。</p>
 */
public interface UserQueryCacheGateway {

    Optional<?> findById(Integer id);

    Optional<?> findByUsername(String username);

    Optional<Integer> findIdByUsername(String username);

    Optional<String> findUsernameById(Integer id);

    List<Integer> findAllUserId();

    List<String> findAllUsername();

    PaginationResultEntity<?> page(PagingQuery<GenericConditionalQuery> query);

    List<SimpleResultCO> allUser();

    List<Integer> getUserRoleIds(Integer userId);

    Boolean isUsernameExist(String username);

    Optional<Integer> getUserStatus(Integer id);
}

