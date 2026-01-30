package edu.cuit.bc.evaluation.application.port;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 评教记录“得分计算/指标映射”等聚合查询端口（读侧）。
 *
 * <p>仅用于接口细分与依赖收敛，不改任何业务语义。</p>
 */
public interface EvaRecordScoreQueryPort {
    Optional<Double> getScoreFromRecord(String prop);

    Optional<Double> getScoreByProp(String prop);

    List<Double> getScoresByProp(String props);

    Map<String, Double> getScorePropMapByProp(String props);
}
