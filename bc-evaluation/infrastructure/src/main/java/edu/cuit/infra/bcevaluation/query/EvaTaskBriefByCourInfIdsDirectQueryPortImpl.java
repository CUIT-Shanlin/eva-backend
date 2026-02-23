package edu.cuit.infra.bcevaluation.query;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.evaluation.application.port.EvaTaskBriefByCourInfIdsDirectQueryPort;
import edu.cuit.client.dto.clientobject.eva.EvaTaskBriefCO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 评教任务“最小视图”直查端口适配器：按 courInfIds 查询任务列表（保持行为不变）。
 *
 * <p>约束：不引入缓存/切面副作用；查询条件与结果顺序保持与旧 Mapper 调用一致。</p>
 */
@Component
@RequiredArgsConstructor
public class EvaTaskBriefByCourInfIdsDirectQueryPortImpl implements EvaTaskBriefByCourInfIdsDirectQueryPort {

    private final EvaTaskMapper evaTaskMapper;

    @Override
    public List<EvaTaskBriefCO> findTaskBriefListByCourInfIds(List<Integer> courInfIds) {
        if (courInfIds == null || courInfIds.isEmpty()) {
            return List.of();
        }

        QueryWrapper<EvaTaskDO> qw = new QueryWrapper<>();
        qw.in("cour_inf_id", courInfIds);

        List<EvaTaskDO> taskDOS = evaTaskMapper.selectList(qw);
        if (taskDOS == null || taskDOS.isEmpty()) {
            return List.of();
        }

        return taskDOS.stream()
                .map(taskDO -> new EvaTaskBriefCO()
                        .setId(taskDO.getId())
                        .setTeacherId(taskDO.getTeacherId())
                        .setCourInfId(taskDO.getCourInfId())
                        .setStatus(taskDO.getStatus()))
                .toList();
    }
}

