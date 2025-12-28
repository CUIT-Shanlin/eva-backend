package edu.cuit.bc.evaluation.application.port;

import edu.cuit.domain.entity.eva.EvaRecordEntity;

import java.util.List;

/**
 * 评教记录“用户日志/被评教日志”等聚合查询端口（读侧）。
 *
 * <p>仅用于接口细分与依赖收敛，不改任何业务语义。</p>
 */
public interface EvaRecordUserLogQueryPort {
    List<EvaRecordEntity> getEvaLogInfo(Integer userId, Integer id, String keyword);

    List<EvaRecordEntity> getEvaEdLogInfo(Integer userId, Integer semId, Integer courseId);
}
