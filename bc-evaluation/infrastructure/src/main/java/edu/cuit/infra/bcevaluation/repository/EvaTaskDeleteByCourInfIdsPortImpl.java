package edu.cuit.infra.bcevaluation.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.evaluation.application.port.EvaTaskDeleteByCourInfIdsPort;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 评教任务“按 courInfIds 删除”端口适配器（保持行为不变）。
 *
 * <p>约束：不引入缓存/切面副作用；入参为空应为 no-op。</p>
 */
@Component
@RequiredArgsConstructor
public class EvaTaskDeleteByCourInfIdsPortImpl implements EvaTaskDeleteByCourInfIdsPort {

    private final EvaTaskMapper evaTaskMapper;

    @Override
    public void deleteByCourInfIds(List<Integer> courInfIds) {
        if (courInfIds == null) {
            return;
        }

        evaTaskMapper.delete(new QueryWrapper<EvaTaskDO>()
                .in("cour_inf_id", courInfIds));
    }
}

