package edu.cuit.infra.bcevaluation.query;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.evaluation.application.port.EvaTaskByTeacherIdsAndStatusDirectQueryPort;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 评教任务直查端口适配器（保持行为不变）。
 *
 * <p>约束：不引入缓存/切面副作用；查询条件与异常行为需与旧 Mapper 调用保持一致；入参为空应为 no-op。</p>
 */
@Component
@RequiredArgsConstructor
public class EvaTaskByTeacherIdsAndStatusDirectQueryPortImpl
        implements EvaTaskByTeacherIdsAndStatusDirectQueryPort {

    private final EvaTaskMapper evaTaskMapper;

    @Override
    public List<Integer> findCourInfIdsByTeacherIdsAndStatus(List<Integer> teacherIds, Integer status) {
        if (teacherIds == null || teacherIds.isEmpty() || status == null) {
            return List.of();
        }

        List<EvaTaskDO> taskDOS = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>()
                .in("teacher_id", teacherIds)
                .eq("status", status));
        if (taskDOS == null || taskDOS.isEmpty()) {
            return List.of();
        }

        return taskDOS.stream()
                .map(EvaTaskDO::getCourInfId)
                .toList();
    }

    @Override
    public Integer findTeacherIdByCourInfIdAndTeacherIds(Integer courInfId, List<Integer> teacherIds) {
        if (courInfId == null || teacherIds == null || teacherIds.isEmpty()) {
            return null;
        }

        EvaTaskDO taskDO = evaTaskMapper.selectOne(new QueryWrapper<EvaTaskDO>()
                .eq("cour_inf_id", courInfId)
                .in("teacher_id", teacherIds));
        if (taskDO == null) {
            return null;
        }

        return taskDO.getTeacherId();
    }
}

