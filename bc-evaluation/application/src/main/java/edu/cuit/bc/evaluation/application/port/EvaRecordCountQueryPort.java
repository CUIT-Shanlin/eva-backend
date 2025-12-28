package edu.cuit.bc.evaluation.application.port;

import java.util.Optional;

/**
 * 评教记录“数量统计”等聚合查询端口（读侧）。
 *
 * <p>仅用于接口细分与依赖收敛，不改任何业务语义。</p>
 */
public interface EvaRecordCountQueryPort {
    Optional<Integer> getEvaNumByCourInfo(Integer courInfId);

    Optional<Integer> getEvaNumByCourse(Integer courseId);
}
