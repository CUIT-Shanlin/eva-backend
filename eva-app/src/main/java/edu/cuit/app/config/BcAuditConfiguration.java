package edu.cuit.app.config;

import edu.cuit.bc.audit.application.port.LogInsertionPort;
import edu.cuit.bc.audit.application.usecase.InsertLogUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * bc-audit 的组合根（单体阶段由 eva-app 负责装配）。
 *
 * <p>提交点 A：仅创建 Maven 模块落点与 wiring，不迁移业务语义。</p>
 */
@Configuration
public class BcAuditConfiguration {
    @Bean
    public InsertLogUseCase insertLogUseCase(LogInsertionPort insertionPort) {
        return new InsertLogUseCase(insertionPort);
    }
}
