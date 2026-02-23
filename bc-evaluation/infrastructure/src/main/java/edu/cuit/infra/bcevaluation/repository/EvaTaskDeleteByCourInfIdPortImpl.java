package edu.cuit.infra.bcevaluation.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.evaluation.application.port.EvaTaskDeleteByCourInfIdPort;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 评教任务“按 courInfId 删除”端口适配器（保持行为不变）。
 *
 * <p>约束：不引入缓存/切面副作用；入参为空应为 no-op。</p>
 */
@Component
@RequiredArgsConstructor
public class EvaTaskDeleteByCourInfIdPortImpl implements EvaTaskDeleteByCourInfIdPort {

    private final EvaTaskMapper evaTaskMapper;

    @Override
    public void deleteByCourInfId(Integer courInfId) {
        if (courInfId == null) {
            return;
        }

        evaTaskMapper.delete(new QueryWrapper<EvaTaskDO>()
                .eq("cour_inf_id", courInfId));
    }
}
