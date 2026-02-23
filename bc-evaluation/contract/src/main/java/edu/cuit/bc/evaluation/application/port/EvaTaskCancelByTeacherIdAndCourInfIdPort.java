package edu.cuit.bc.evaluation.application.port;

/**
 * 评教任务“取消”端口（写侧）。
 *
 * <p>用于跨 BC 在课程侧“修改课次/时间导致取消评教任务”等链路中，按 teacherId + courInfId 将评教任务置为取消状态。
 * 当前约定：将 eva_task.status 更新为 {@code 2}（保持旧链路语义不变）。</p>
 *
 * <p>约束：实现侧不引入缓存/切面副作用；入参为空应为 no-op。</p>
 */
public interface EvaTaskCancelByTeacherIdAndCourInfIdPort {

    void cancelByTeacherIdAndCourInfId(Integer teacherId, Integer courInfId);
}

