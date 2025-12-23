package edu.cuit.infra.bcevaluation.query;

import edu.cuit.bc.evaluation.application.port.EvaTaskQueryPort;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.EvaTaskConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.eva.EvaTaskEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 评教任务读侧查询端口实现（委托 QueryRepo，保持行为不变）。
 */
@Component
@RequiredArgsConstructor
public class EvaTaskQueryPortImpl implements EvaTaskQueryPort {
    private final EvaTaskQueryRepo repo;

    @Override
    public PaginationResultEntity<EvaTaskEntity> pageEvaUnfinishedTask(Integer semId, PagingQuery<EvaTaskConditionalQuery> taskQuery) {
        return repo.pageEvaUnfinishedTask(semId, taskQuery);
    }

    @Override
    public List<EvaTaskEntity> evaSelfTaskInfo(Integer useId, Integer id, String keyword) {
        return repo.evaSelfTaskInfo(useId, id, keyword);
    }

    @Override
    public Optional<EvaTaskEntity> oneEvaTaskInfo(Integer id) {
        return repo.oneEvaTaskInfo(id);
    }

    @Override
    public Optional<Integer> getEvaNumber(Long id) {
        return repo.getEvaNumber(id);
    }

    @Override
    public Optional<String> getNameByTaskId(Integer taskId) {
        return repo.getNameByTaskId(taskId);
    }
}
