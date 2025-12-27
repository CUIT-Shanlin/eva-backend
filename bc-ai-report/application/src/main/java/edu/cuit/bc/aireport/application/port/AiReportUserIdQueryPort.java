package edu.cuit.bc.aireport.application.port;

import java.util.Optional;

/**
 * AI 报告：按用户名查询 userId 的端口（过渡期）。
 *
 * <p>保持行为不变：端口仅用于隔离依赖，具体查询逻辑在端口适配器中原样执行。</p>
 */
public interface AiReportUserIdQueryPort {
    Optional<Integer> findIdByUsername(String username);
}
