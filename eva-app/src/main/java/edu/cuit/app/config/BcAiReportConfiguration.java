package edu.cuit.app.config;

import edu.cuit.bc.aireport.application.port.AiReportDocExportPort;
import edu.cuit.bc.aireport.application.usecase.ExportAiReportDocByUsernameUseCase;
import edu.cuit.bc.aireport.application.usecase.ExportAiReportDocUseCase;
import edu.cuit.client.api.ai.IAiCourseAnalysisService;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * bc-ai-report 的组合根（单体阶段由 eva-app 负责装配）。
 *
 * <p>提交点 A：仅创建 Maven 模块落点与 wiring，不迁移业务语义。</p>
 */
@Configuration
public class BcAiReportConfiguration {
    @Bean
    public ExportAiReportDocUseCase exportAiReportDocUseCase(AiReportDocExportPort exportPort) {
        return new ExportAiReportDocUseCase(exportPort);
    }

    @Bean
    public ExportAiReportDocByUsernameUseCase exportAiReportDocByUsernameUseCase(
            UserQueryGateway userQueryGateway,
            @Lazy IAiCourseAnalysisService aiCourseAnalysisService,
            ExportAiReportDocUseCase exportAiReportDocUseCase
    ) {
        return new ExportAiReportDocByUsernameUseCase(userQueryGateway, aiCourseAnalysisService, exportAiReportDocUseCase);
    }
}
