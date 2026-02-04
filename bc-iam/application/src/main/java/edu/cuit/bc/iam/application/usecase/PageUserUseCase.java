package edu.cuit.bc.iam.application.usecase;

import edu.cuit.bc.iam.application.port.UserDirectoryPageQueryPort;
import edu.cuit.bc.iam.application.port.UserEntityQueryPort;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;

import java.util.function.Function;

/**
 * 用户分页查询用例（读模型入口）。
 *
 * <p>说明：现阶段仅做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public class PageUserUseCase {
    private final Function<PagingQuery<GenericConditionalQuery>, PaginationResultEntity<?>> pageQuery;

    public PageUserUseCase(UserDirectoryPageQueryPort queryPort) {
        this.pageQuery = queryPort::page;
    }

    /**
     * 过渡期保持旧语义：此构造仅用于复用现有 wiring/测试（不在用例内暴露旧领域实体类型）。
     */
    public PageUserUseCase(UserEntityQueryPort legacyQueryPort) {
        this.pageQuery = legacyQueryPort::page;
    }

    @SuppressWarnings("unchecked")
    public <T> PaginationResultEntity<T> execute(PagingQuery<GenericConditionalQuery> query) {
        return (PaginationResultEntity<T>) pageQuery.apply(query);
    }
}
