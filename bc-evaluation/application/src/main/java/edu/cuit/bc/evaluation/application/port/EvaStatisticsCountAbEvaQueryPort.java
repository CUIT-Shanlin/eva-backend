package edu.cuit.bc.evaluation.application.port;

import java.util.List;

/**
 * 评教统计“异常评教数量”查询端口（读侧）。
 *
 * <p>仅用于接口细分与依赖收敛，不改任何业务语义。</p>
 */
public interface EvaStatisticsCountAbEvaQueryPort {

    List<Integer> getCountAbEva(Integer semId, Integer userId);
}

