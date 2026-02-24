package edu.cuit.infra.bcevaluation.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.evaluation.application.port.FormRecordDeleteByTaskIdsPort;
import edu.cuit.infra.dal.database.dataobject.eva.FormRecordDO;
import edu.cuit.infra.dal.database.mapper.eva.FormRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 评教记录“按 taskIds 删除”端口适配器（保持行为不变）。
 *
 * <p>约束：不引入缓存/切面副作用；入参为空或空列表应为 no-op。</p>
 */
@Component
@RequiredArgsConstructor
public class FormRecordDeleteByTaskIdsPortImpl implements FormRecordDeleteByTaskIdsPort {

    private final FormRecordMapper formRecordMapper;

    @Override
    public void deleteByTaskIds(List<Integer> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return;
        }

        formRecordMapper.delete(new QueryWrapper<FormRecordDO>()
                .in("task_id", taskIds));
    }
}

