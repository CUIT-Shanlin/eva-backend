package edu.cuit.adapter.controller.eva.query;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.cuit.client.api.eva.IEvaStatisticsService;
import edu.cuit.client.dto.clientobject.eva.ScoreRangeCourseCO;
import edu.cuit.client.dto.clientobject.eva.*;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评教统计相关
 */
@RestController
@RequiredArgsConstructor
@Validated
public class EvaStatisticsController {

    private final IEvaStatisticsService iEvaStatisticsService;

    // 评教记录相关统计

    /**
     * 获取评教分数统计基础信息
     * @param score 指定分数
     * @param semId 学期id
     */
    @GetMapping("/evaluate/score/situation")
    @SaCheckPermission("evaluate.score.query")
    public CommonResult<EvaScoreInfoCO> evaScoreStatisticsInfo(
            @RequestParam (value = "semId",required = false) Integer semId,
            @RequestParam ("score") Number score){
        return CommonResult.success(iEvaStatisticsService.evaScoreStatisticsInfo(semId,score));
    }
    /**
     * 获取评教任务完成情况
     * @param semId 学期id
     */
    @GetMapping("/evaluate/task/situation")
    @SaCheckPermission("evaluate.task.situation.query")
    public CommonResult<EvaSituationCO> evaTemplateSituation(
            @RequestParam (value = "semId",required = false) Integer semId){
        return CommonResult.success(iEvaStatisticsService.evaTemplateSituation(semId));
    }
    // 评教看板相关

    /**
     * 获取指定某一天的详细评教统计数据
     * @param day 指定的这一天和今天相差多少天，eg：0 =》 今天，-1 =》 昨天
     * @param num 要将这一天的24小时分几段时间进行数据的统计
     * @param semId 学期id
     */
    @GetMapping("/evaluate/moreCount/{day}/{num}")
    @SaCheckPermission("evaluate.board.query")
    public CommonResult<OneDayAddEvaDataCO> evaOneDayInfo(
            @PathVariable ("day") Integer day,
            @PathVariable ("num") Integer num,
            @RequestParam (value = "semId",required = false) Integer semId){
        return CommonResult.success(iEvaStatisticsService.evaOneDayInfo(day, num, semId));
    }
    /**
     * 获取各个分数段中 课程的数目情况
     * @param num 获取多少个分数段的数据，分数段截取后段，如果有某个分数段 课程数目为0，应当忽略掉，不参与计算
     * @param interval 间隔，分数段之间的默认间隔，如果按照该间隔，无法达到 num 个有数据的分数段，则将间隔减少0.2分，直到达到 num 个分数段
     */
    @GetMapping("/evaluate/score/count/{num}/{interval}")
    public CommonResult<List<ScoreRangeCourseCO>> scoreRangeCourseInfo(
            @PathVariable ("num") Integer num,
            @PathVariable ("interval") Integer interval){
        return CommonResult.success(iEvaStatisticsService.scoreRangeCourseInfo(num, interval));
    }
    /**
     * 获取上个月和本月的评教数目，以有两个整数的List<Integer>形式返回，data[0]：上个月评教数目；data[1]：本月评教数目
     * @param semId 学期id
     */
    @GetMapping("/evaluate/month/count")
    public CommonResult<List<Integer>> getMonthEvaNUmber(
            @RequestParam(value = "semId",required = false) Integer semId){
        return CommonResult.success(iEvaStatisticsService.getMonthEvaNUmber(semId));
    }
    /**
     * 获取指定过去一段时间内的详细评教统计数据
     * @param num 获取从今天开始往过去看 num 天（含今天）中，每天的新增评教数目
     * @param target 被评教的目标次数，大于等于该数目则达标，小于则未达标
     * @param evaTarget 评教的目标次数，大于等于该数目则达标，小于则未达标
     * @param semId 学期id
     */
    @GetMapping("/evaluate/moreCounts/{num}/{target}/{evaTarget}")
    public CommonResult<PastTimeEvaDetailCO> getEvaData(
            @RequestParam(value = "semId",required = false) Integer semId,
            @PathVariable ("num") Integer num,
            @PathVariable ("target") Integer target,
            @PathVariable ("evaTarget") Integer evaTarget){
        return CommonResult.success(iEvaStatisticsService.getEvaData(semId, num, target, evaTarget));
    }
}
