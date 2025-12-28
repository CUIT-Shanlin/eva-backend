package edu.cuit.app.config;

import edu.cuit.bc.evaluation.application.port.AddEvaTemplateRepository;
import edu.cuit.bc.evaluation.application.port.DeleteEvaRecordRepository;
import edu.cuit.bc.evaluation.application.port.DeleteEvaTemplateRepository;
import edu.cuit.bc.evaluation.application.port.DomainEventPublisher;
import edu.cuit.bc.evaluation.application.port.PostEvaTaskRepository;
import edu.cuit.bc.evaluation.application.port.SubmitEvaluationRepository;
import edu.cuit.bc.evaluation.application.port.UpdateEvaTemplateRepository;
import edu.cuit.bc.evaluation.application.usecase.AddEvaTemplateUseCase;
import edu.cuit.bc.evaluation.application.usecase.DeleteEvaRecordUseCase;
import edu.cuit.bc.evaluation.application.usecase.DeleteEvaTemplateUseCase;
import edu.cuit.bc.evaluation.application.usecase.EvaStatisticsQueryUseCase;
import edu.cuit.bc.evaluation.application.usecase.PostEvaTaskUseCase;
import edu.cuit.bc.evaluation.application.usecase.SubmitEvaluationUseCase;
import edu.cuit.bc.evaluation.application.usecase.UpdateEvaTemplateUseCase;
import edu.cuit.bc.evaluation.application.port.EvaStatisticsOverviewQueryPort;
import edu.cuit.bc.evaluation.application.port.EvaStatisticsTrendQueryPort;
import edu.cuit.bc.evaluation.application.port.EvaStatisticsUnqualifiedUserQueryPort;
import edu.cuit.domain.gateway.eva.EvaConfigGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * bc-evaluation 的组合根（单体阶段由 eva-app 负责装配）。
 *
 * <p>未来拆分微服务时：可以把该配置迁移到对应服务的 bootstrap 模块中。</p>
 */
@Configuration
public class BcEvaluationConfiguration {
    @Bean
    public SubmitEvaluationUseCase submitEvaluationUseCase(
            SubmitEvaluationRepository repository,
            DomainEventPublisher eventPublisher
    ) {
        return new SubmitEvaluationUseCase(repository, eventPublisher);
    }

    @Bean
    public PostEvaTaskUseCase postEvaTaskUseCase(
            PostEvaTaskRepository repository,
            DomainEventPublisher eventPublisher
    ) {
        return new PostEvaTaskUseCase(repository, eventPublisher);
    }

    @Bean
    public DeleteEvaRecordUseCase deleteEvaRecordUseCase(DeleteEvaRecordRepository repository) {
        return new DeleteEvaRecordUseCase(repository);
    }

    @Bean
    public DeleteEvaTemplateUseCase deleteEvaTemplateUseCase(DeleteEvaTemplateRepository repository) {
        return new DeleteEvaTemplateUseCase(repository);
    }

    @Bean
    public AddEvaTemplateUseCase addEvaTemplateUseCase(AddEvaTemplateRepository repository) {
        return new AddEvaTemplateUseCase(repository);
    }

    @Bean
    public UpdateEvaTemplateUseCase updateEvaTemplateUseCase(UpdateEvaTemplateRepository repository) {
        return new UpdateEvaTemplateUseCase(repository);
    }

    @Bean
    public EvaStatisticsQueryUseCase evaStatisticsQueryUseCase(
            EvaStatisticsOverviewQueryPort overviewQueryPort,
            EvaStatisticsTrendQueryPort trendQueryPort,
            EvaStatisticsUnqualifiedUserQueryPort unqualifiedUserQueryPort,
            EvaConfigGateway evaConfigGateway
    ) {
        return new EvaStatisticsQueryUseCase(overviewQueryPort, trendQueryPort, unqualifiedUserQueryPort, evaConfigGateway);
    }
}
