package edu.cuit.app.bcaireport.adapter;

import edu.cuit.bc.aireport.application.port.AiReportUserIdQueryPort;
import edu.cuit.bc.iam.application.port.UserBasicQueryPort;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * AI 报告：按用户名查询 userId 的端口适配器（过渡期实现）。
 *
 * <p>保持行为不变：原样委托 {@link UserBasicQueryPort#findIdByUsername(String)}。</p>
 */
@Component
public class AiReportUserIdQueryPortImpl implements AiReportUserIdQueryPort {
    private final UserBasicQueryPort userBasicQueryPort;

    public AiReportUserIdQueryPortImpl(UserBasicQueryPort userBasicQueryPort) {
        this.userBasicQueryPort = Objects.requireNonNull(userBasicQueryPort, "userBasicQueryPort");
    }

    @Override
    public Optional<Integer> findIdByUsername(String username) {
        return userBasicQueryPort.findIdByUsername(username);
    }
}
