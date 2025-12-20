package edu.cuit.bc.evaluation.application.usecase;

import edu.cuit.bc.evaluation.application.model.UpdateEvaTemplateCommand;
import edu.cuit.bc.evaluation.application.port.UpdateEvaTemplateRepository;

import java.util.Objects;

/**
 * 修改评教模板用例（写模型入口）。
 *
 * <p>保持行为不变：用例仅做编排与依赖隔离，具体校验与落库逻辑在端口适配器中原样搬运。</p>
 */
public class UpdateEvaTemplateUseCase {
    private final UpdateEvaTemplateRepository repository;

    public UpdateEvaTemplateUseCase(UpdateEvaTemplateRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public void update(UpdateEvaTemplateCommand command) {
        repository.update(command);
    }
}

