package edu.cuit.bc.evaluation.application.port;

import java.util.List;

/**
 * 评教任务“级联删除”端口（写侧）。
 *
 * <p>用于跨 BC 在删除链路中按 taskIds 级联删除评教任务与其表单记录：
 * 先删 eva_task，再删 form_record（保持行为不变）。</p>
 *
 * <p>约束：实现侧不引入缓存/切面副作用；空入参应为 no-op。</p>
 */
public interface EvaTaskCascadeDeleteByTaskIdsPort {

    void deleteCascadeByTaskIds(List<Integer> taskIds);
}

