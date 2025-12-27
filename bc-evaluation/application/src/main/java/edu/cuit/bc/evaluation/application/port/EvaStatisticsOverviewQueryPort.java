package edu.cuit.bc.evaluation.application.port;

import edu.cuit.client.dto.clientobject.eva.EvaScoreInfoCO;
import edu.cuit.client.dto.clientobject.eva.EvaSituationCO;
import edu.cuit.client.dto.clientobject.eva.PastTimeEvaDetailCO;
import edu.cuit.client.dto.clientobject.eva.ScoreRangeCourseCO;

import java.util.List;
import java.util.Optional;

/**
 * 评教统计“概览/区间/历史明细”等聚合查询端口（读侧）。
 *
 * <p>仅用于接口细分与依赖收敛，不改任何业务语义。</p>
 */
public interface EvaStatisticsOverviewQueryPort {
    Optional<EvaScoreInfoCO> evaScoreStatisticsInfo(Integer semId, Number score);

    Optional<EvaSituationCO> evaTemplateSituation(Integer semId);

    List<ScoreRangeCourseCO> scoreRangeCourseInfo(Integer num, Integer interval);

    Optional<PastTimeEvaDetailCO> getEvaData(Integer semId, Integer num, Integer target, Integer evaTarget);

    List<Integer> getCountAbEva(Integer semId, Integer userId);
}
