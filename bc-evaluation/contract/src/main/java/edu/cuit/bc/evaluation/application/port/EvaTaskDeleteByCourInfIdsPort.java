package edu.cuit.bc.evaluation.application.port;

import java.util.List;

/**
 * 评教任务“按 courInfIds 删除”端口（写侧）。
 *
 * <p>用于跨 BC 在课程删课等链路中，按 courInfIds 批量删除评教任务。</p>
 *
 * <p>约束：实现侧不引入缓存/切面副作用；保持与旧 Mapper 调用一致（仅删除 eva_task，不做级联删除）；入参为空应为 no-op。</p>
 */
public interface EvaTaskDeleteByCourInfIdsPort {

    void deleteByCourInfIds(List<Integer> courInfIds);
}

