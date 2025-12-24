package edu.cuit.app.bcaireport.adapter;

import edu.cuit.bc.aireport.application.port.AiReportUserIdQueryPort;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * AI 报告：按用户名查询 userId 的端口适配器（过渡期实现）。
 *
 * <p>保持行为不变：原样委托 {@link UserQueryGateway#findIdByUsername(String)}。</p>
 */
@Component
public class AiReportUserIdQueryPortImpl implements AiReportUserIdQueryPort {
    private final UserQueryGateway userQueryGateway;

    public AiReportUserIdQueryPortImpl(UserQueryGateway userQueryGateway) {
        this.userQueryGateway = Objects.requireNonNull(userQueryGateway, "userQueryGateway");
    }

    @Override
    public Optional<Integer> findIdByUsername(String username) {
        return userQueryGateway.findIdByUsername(username);
    }
}
