package edu.cuit.infra.bcevaluation.repository;

import edu.cuit.bc.evaluation.application.port.EvaTaskInsertPort;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 评教任务“插入并回传 taskId”端口适配器（保持行为不变）。
 *
 * <p>约束：不引入缓存/切面副作用；插入语义与主键回填时机需与旧 Mapper insert 保持一致；入参为空应为 no-op。</p>
 */
@Component
@RequiredArgsConstructor
public class EvaTaskInsertPortImpl implements EvaTaskInsertPort {

    private final EvaTaskMapper evaTaskMapper;

    @Override
    public Integer insertAndReturnId(
            Integer teacherId,
            Integer courInfId,
            Integer status,
            LocalDateTime createTime,
            LocalDateTime updateTime
    ) {
        if (teacherId == null || courInfId == null || status == null || createTime == null || updateTime == null) {
            return null;
        }

        EvaTaskDO evaTaskDO = new EvaTaskDO();
        evaTaskDO.setTeacherId(teacherId);
        evaTaskDO.setCourInfId(courInfId);
        evaTaskDO.setStatus(status);
        evaTaskDO.setCreateTime(createTime);
        evaTaskDO.setUpdateTime(updateTime);

        evaTaskMapper.insert(evaTaskDO);
        return evaTaskDO.getId();
    }
}

