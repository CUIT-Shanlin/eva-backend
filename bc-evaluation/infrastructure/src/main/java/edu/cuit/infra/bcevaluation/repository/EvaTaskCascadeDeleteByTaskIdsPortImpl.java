package edu.cuit.infra.bcevaluation.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.evaluation.application.port.EvaTaskCascadeDeleteByTaskIdsPort;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormRecordDO;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormRecordMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 评教任务“级联删除”端口适配器（保持行为不变）。
 *
 * <p>约束：不引入缓存/切面副作用；删除顺序保持“先 eva_task 后 form_record”。</p>
 */
@Component
@RequiredArgsConstructor
public class EvaTaskCascadeDeleteByTaskIdsPortImpl implements EvaTaskCascadeDeleteByTaskIdsPort {

    private final EvaTaskMapper evaTaskMapper;
    private final FormRecordMapper formRecordMapper;

    @Override
    public void deleteCascadeByTaskIds(List<Integer> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return;
        }

        evaTaskMapper.delete(new QueryWrapper<EvaTaskDO>().in("id", taskIds));
        formRecordMapper.delete(new QueryWrapper<FormRecordDO>().in("task_id", taskIds));
    }
}

