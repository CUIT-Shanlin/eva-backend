package edu.cuit.bc.evaluation.application.port;

import edu.cuit.client.dto.clientobject.eva.EvaWeekAddCO;

import java.util.List;
import java.util.Optional;

/**
 * 评教统计“趋势/周期”查询端口（读侧）。
 *
 * <p>仅用于接口细分与依赖收敛，不改任何业务语义。</p>
 */
public interface EvaStatisticsTrendQueryPort {
    Optional<EvaWeekAddCO> evaWeekAdd(Integer week, Integer semId);

    List<Integer> getMonthEvaNUmber(Integer semId);
}
