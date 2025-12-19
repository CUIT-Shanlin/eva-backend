package edu.cuit.bc.evaluation.application.usecase;

import edu.cuit.bc.evaluation.application.port.DeleteEvaRecordRepository;

import java.util.List;
import java.util.Objects;

/**
 * 删除评教记录用例（写模型入口）。
 *
 * <p>保持行为不变：用例仅做编排与依赖隔离，具体校验与落库逻辑在端口适配器中原样搬运。</p>
 */
public class DeleteEvaRecordUseCase {
    private final DeleteEvaRecordRepository repository;

    public DeleteEvaRecordUseCase(DeleteEvaRecordRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public void delete(List<Integer> ids) {
        repository.delete(ids);
    }
}

