package edu.cuit.client.api.eva;

import edu.cuit.client.dto.clientobject.eva.*;

/**
 * 评教统计相关业务接口
 */
public interface IEvaStatisticsService {
    /**
     * 获取评教分数统计基础信息
     * @param score 指定分数
     * @param semId 学期id
     */
    EvaScoreInfoCO evaScoreStatisticsInfo(Integer semId,Number score);

    /**
     * 获取评教任务完成情况
     * @param semId 学期id
     */
    EvaSituationCO evaTemplateSituation(Integer semId);

    /**
     * 获取指定某一天的详细评教统计数据
     * @param day 指定的这一天和今天相差多少天，eg：0 =》 今天，-1 =》 昨天
     * @param num 要将这一天的24小时分几段时间进行数据的统计
     * @param semId 学期id
     */
    OneDayAddEvaDataCO evaOneDayInfo(Integer day, Integer num, Integer semId);

    /**
     * 获取各个分数段中 课程的数目情况
     * @param num 获取多少个分数段的数据，分数段截取后段，如果有某个分数段 课程数目为0，应当忽略掉，不参与计算
     * @param interval 间隔，分数段之间的默认间隔，如果按照该间隔，无法达到 num 个有数据的分数段，则将间隔减少0.2分，直到达到 num 个分数段
     */
    ScoreRangeCourseCO scoreRangeCourseInfo(Integer num, Integer interval);

    /**
     * 获取上个月和本月的评教数目，以有两个整数的List<Integer>形式返回，data[0]：上个月评教数目；data[1]：本月评教数目
     * @param semId 学期id
     */
    Void getMonthEvaNUmber(Integer semId);

    /**
     * 获取指定过去一段时间内的详细评教统计数据
     * @param num 获取从今天开始往过去看 num 天（含今天）中，每天的新增评教数目
     * @param target 被评教的目标次数，大于等于该数目则达标，小于则未达标
     * @param evaTarget 评教的目标次数，大于等于该数目则达标，小于则未达标
     * @param semId 学期id
     */
    PastTimeEvaDetailCO getEvaData(Integer semId, Integer num, Integer target, Integer evaTarget);
}
