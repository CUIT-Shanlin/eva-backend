package edu.cuit.bc.evaluation.application.port;

/**
 * 评教任务“按 courInfId 删除”端口（写侧）。
 *
 * <p>用于跨 BC 在课程侧“自助改课导致删除某些课次（cour_inf）”等链路中，删除对应 courInfId 的评教任务。</p>
 *
 * <p>约束：实现侧不引入缓存/切面副作用；保持与旧 Mapper 调用一致（仅删除 eva_task，不做级联删除）；入参为空应为 no-op。</p>
 */
public interface EvaTaskDeleteByCourInfIdPort {

    void deleteByCourInfId(Integer courInfId);
}

