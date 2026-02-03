package edu.cuit.infra.bciam.adapter;

import edu.cuit.bc.iam.application.port.UserDirectoryPageQueryPort;
import edu.cuit.client.dto.clientobject.SimpleResultCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * bc-iam：用户目录/分页查询端口适配器（保持历史行为不变）。
 *
 * <p>保持缓存/切面触发点不变：内部委托旧 {@link UserQueryGateway}（其仍承载历史缓存注解与切面触发点）。</p>
 */
@Component
@RequiredArgsConstructor
public class UserDirectoryPageQueryPortImpl implements UserDirectoryPageQueryPort {

    private final UserQueryGateway userQueryGateway;

    @Override
    public PaginationResultEntity<?> page(PagingQuery<GenericConditionalQuery> query) {
        return userQueryGateway.page(query);
    }

    @Override
    public List<SimpleResultCO> allUser() {
        return userQueryGateway.allUser();
    }

    @Override
    public List<String> findAllUsername() {
        return userQueryGateway.findAllUsername();
    }
}

