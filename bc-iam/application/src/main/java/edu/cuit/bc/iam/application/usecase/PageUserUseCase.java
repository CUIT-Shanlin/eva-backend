package edu.cuit.bc.iam.application.usecase;

import edu.cuit.bc.iam.application.port.UserEntityQueryPort;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.user.biz.UserEntity;

/**
 * 用户分页查询用例（读模型入口）。
 *
 * <p>说明：现阶段仅做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class PageUserUseCase {
    private final UserEntityQueryPort queryPort;

    public PageUserUseCase(UserEntityQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public PaginationResultEntity<UserEntity> execute(PagingQuery<GenericConditionalQuery> query) {
        return queryPort.page(query);
    }
}

