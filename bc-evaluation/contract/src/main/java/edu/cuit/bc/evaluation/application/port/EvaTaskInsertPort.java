package edu.cuit.bc.evaluation.application.port;

import java.time.LocalDateTime;

/**
 * 评教任务写端口：插入 eva_task 并返回生成的 taskId（用于跨 BC 写侧收敛，保持行为不变）。
 *
 * <p>约束：实现侧不引入缓存/切面副作用；插入语义与主键回填时机需与旧 Mapper insert 保持一致；入参为空应为 no-op。</p>
 */
public interface EvaTaskInsertPort {

    /**
     * 插入评教任务并返回生成的主键 ID（taskId）。
     *
     * @return 生成的 taskId；若入参为空导致 no-op，则返回 null
     */
    Integer insertAndReturnId(
            Integer teacherId,
            Integer courInfId,
            Integer status,
            LocalDateTime createTime,
            LocalDateTime updateTime
    );
}

