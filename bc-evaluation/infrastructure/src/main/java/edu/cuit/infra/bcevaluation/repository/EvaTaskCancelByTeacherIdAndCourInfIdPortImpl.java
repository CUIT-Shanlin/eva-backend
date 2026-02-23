package edu.cuit.infra.bcevaluation.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.evaluation.application.port.EvaTaskCancelByTeacherIdAndCourInfIdPort;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 评教任务“取消”端口适配器（保持行为不变）。
 *
 * <p>约束：不引入缓存/切面副作用；入参为空应为 no-op。</p>
 */
@Component
@RequiredArgsConstructor
public class EvaTaskCancelByTeacherIdAndCourInfIdPortImpl implements EvaTaskCancelByTeacherIdAndCourInfIdPort {

    private final EvaTaskMapper evaTaskMapper;

    @Override
    public void cancelByTeacherIdAndCourInfId(Integer teacherId, Integer courInfId) {
        if (teacherId == null || courInfId == null) {
            return;
        }

        EvaTaskDO update = new EvaTaskDO();
        update.setStatus(2);
        evaTaskMapper.update(update, new QueryWrapper<EvaTaskDO>()
                .eq("teacher_id", teacherId)
                .eq("cour_inf_id", courInfId));
    }
}

