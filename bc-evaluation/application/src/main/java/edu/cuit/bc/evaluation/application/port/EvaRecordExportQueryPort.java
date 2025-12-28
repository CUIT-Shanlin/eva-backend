package edu.cuit.bc.evaluation.application.port;

/**
 * 评教记录“导出链路”所需的聚合查询端口（读侧）。
 *
 * <p>仅用于接口细分与依赖收敛，不改任何业务语义。</p>
 */
public interface EvaRecordExportQueryPort extends EvaRecordCourseQueryPort, EvaRecordScoreQueryPort {
}

