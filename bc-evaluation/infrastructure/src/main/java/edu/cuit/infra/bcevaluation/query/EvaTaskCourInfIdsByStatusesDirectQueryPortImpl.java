package edu.cuit.infra.bcevaluation.query;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.evaluation.application.port.EvaTaskCourInfIdsByStatusesDirectQueryPort;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 评教任务 courInfId 列表直查端口适配器（保持行为不变）。
 *
 * <p>约束：不引入缓存/切面副作用；查询条件、结果顺序与空值语义保持与旧 Mapper 调用一致。</p>
 */
@Component
@RequiredArgsConstructor
public class EvaTaskCourInfIdsByStatusesDirectQueryPortImpl
        implements EvaTaskCourInfIdsByStatusesDirectQueryPort {

    private final EvaTaskMapper evaTaskMapper;

    @Override
    public List<Integer> findCourInfIdsByStatuses(List<Integer> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }

        List<EvaTaskDO> taskDOS = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>()
                .in("status", statuses));
        if (taskDOS == null || taskDOS.isEmpty()) {
            return List.of();
        }

        return taskDOS.stream()
                .map(EvaTaskDO::getCourInfId)
                .toList();
    }
}

