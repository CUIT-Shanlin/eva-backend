package edu.cuit.bc.evaluation.application.port;

import java.util.List;

/**
 * 评教任务直查端口（读侧）：按 teacherIds + status 查询任务关联的 courInfIds / 以及按 courInfId + teacherIds 查询任务 teacherId。
 *
 * <p>用于跨 BC 在课程侧“分配评教老师/时间冲突校验”等链路中，复用评教侧既有查询语义。</p>
 *
 * <p>约束：实现侧不引入缓存/切面副作用；查询条件与异常行为需与旧 Mapper 调用保持一致；入参为空应为 no-op。</p>
 */
public interface EvaTaskByTeacherIdsAndStatusDirectQueryPort {

    /**
     * 按 teacherIds + status 查询评教任务关联的 courInfId 列表。
     */
    List<Integer> findCourInfIdsByTeacherIdsAndStatus(List<Integer> teacherIds, Integer status);

    /**
     * 按 courInfId + teacherIds 查询评教任务的 teacherId（内部保持 selectOne 语义）。
     */
    Integer findTeacherIdByCourInfIdAndTeacherIds(Integer courInfId, List<Integer> teacherIds);
}

