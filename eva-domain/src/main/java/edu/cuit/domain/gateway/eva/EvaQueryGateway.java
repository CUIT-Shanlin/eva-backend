package edu.cuit.domain.gateway.eva;

import edu.cuit.client.dto.clientobject.eva.*;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserInfoCO;
import edu.cuit.client.dto.clientobject.user.UnqualifiedUserResultCO;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.EvaLogConditionalQuery;
import edu.cuit.client.dto.query.condition.EvaTaskConditionalQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.client.dto.query.condition.UnqualifiedUserConditionalQuery;
import edu.cuit.domain.entity.PaginationResultEntity;
import edu.cuit.domain.entity.eva.EvaRecordEntity;
import edu.cuit.domain.entity.eva.EvaTaskEntity;
import edu.cuit.domain.entity.eva.EvaTemplateEntity;
import edu.cuit.zhuyimeng.framework.common.result.CommonResult;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

/**
 * 评教查询相关用户数据接口
 */
@Component
public interface EvaQueryGateway {

    /**
     *分页获取评教记录+条件查询，keyword模糊查询 教学课程
     * @param semId 学期id
     * @param evaLogQuery 查询参数
     * @return List<EvaRecordEntity>
     */
    PaginationResultEntity<EvaRecordEntity> pageEvaRecord(Integer semId, PagingQuery<EvaLogConditionalQuery> evaLogQuery);
    /**
     *分页获取未完成的评教任务+条件查询
     * @param semId 学期id
     * @param taskQuery 查询参数
     * @return List<EvaTaskEntity>
     */
    PaginationResultEntity<EvaTaskEntity> pageEvaUnfinishedTask(Integer semId, PagingQuery<EvaTaskConditionalQuery> taskQuery);
    /**
     *分页获取评教模板信息
     * @param semId 学期id
     * @param query 查询参数
     * @return List<EvaTemplateEntity>
     */
    PaginationResultEntity<EvaTemplateEntity> pageEvaTemplate(Integer semId, PagingQuery<GenericConditionalQuery> query);

    /**
     * 获取自己的所有待办评教任务
     * @param id 学期ID 编号
     * @param keyword 模糊查询课程名称或教学老师姓名
     */
    List<EvaTaskEntity> evaSelfTaskInfo(Integer id, String keyword);
    /**
     * 获取自己的评教记录
     * @param id 学期ID 编号
     * @param keyword 模糊查询课程名称或教学老师姓名
     */
    List<EvaRecordEntity> getEvaLogInfo(Integer id,String keyword);
    /**
     * 获取一个评教任务的详细信息
     * @param id 任务id
     * @return EvaTaskEntity
     */
    Optional<EvaTaskEntity> oneEvaTaskInfo(Integer id);

    /**
     * 获取评教分数统计基础信息
     * @param score 指定分数
     * @param semId 学期id
     * @return EvaScoreInfoCO
     */
    Optional<EvaScoreInfoCO> evaScoreStatisticsInfo(Integer semId, Number score);

    /**
     * 获取上个月和本月的评教数目，以有两个整数的List<Integer>形式返回，data[0]：上个月评教数目；data[1]：本月评教数目
     * @param semId 学期id
     * @return List<Integer>
     */
    List<Integer> getMonthEvaNUmber(Integer semId);

    /**
     * 获取指定过去一段时间内的详细评教统计数据
     * @param num 获取从今天开始往过去看 num 天（含今天）中，每天的新增评教数目
     * @param target 被评教的目标次数，大于等于该数目则达标，小于则未达标
     * @param evaTarget 评教的目标次数，大于等于该数目则达标，小于则未达标
     * @param semId 学期id
     * @return PastTimeEvaDetailCO
     */
    Optional<PastTimeEvaDetailCO> getEvaData(Integer semId, Integer num, Integer target, Integer evaTarget);

    /**
     * 获取指定数量评教任务未达标用户信息
     * @param query 查询对象
     * @param num 前几个用户数据
     * @param target 评教的目标 数目，大于等于该数目则达标，小于则未达标
     */
    Optional<UnqualifiedUserResultCO> getEvaTargetAmountUnqualifiedUser(UnqualifiedUserConditionalQuery query, Integer num, Integer target);

    /**
     * 获取指定数量被评教任务未达标用户信息
     * @param query 查询对象
     * @param num 前几个用户数据
     * @param target 被评教的目标 数目，大于等于该数目则达标，小于则未达标
     */
    Optional<UnqualifiedUserResultCO> getBeEvaTargetAmountUnqualifiedUser(UnqualifiedUserConditionalQuery query,Integer num,Integer target);

    /**
     * 分页获取评教未达标用户
     * @param query 查询对象
     * @param target 评教的目标 数目，大于等于该数目则达标，小于则未达标
     */
    PaginationResultEntity<UnqualifiedUserInfoCO> pageEvaUnqualifiedUserInfo(PagingQuery<UnqualifiedUserConditionalQuery> query, Integer target);

    /**
     * 分页获取被评教未达标用户
     * @param query 查询对象
     * @param target 被评教的目标 数目，大于等于该数目则达标，小于则未达标
     */
    PaginationResultEntity<UnqualifiedUserInfoCO> pageBeEvaUnqualifiedUserInfo(PagingQuery<UnqualifiedUserConditionalQuery> query,Integer target);

    /**
     * 获取用户已评教数目
     * @param id 用户id
     */
    Integer getEvaNumber(Long id);

    /**
     * 获取一个任务对应的评教模板
     * @param taskId 任务id
     * @param semId 学期id
     */

    Optional<String> getTaskTemplate(Integer taskId, Integer semId);
    /**
     * 获取各个分数段中 课程的数目情况
     * @param num 获取多少个分数段的数据，分数段截取后段，如果有某个分数段 课程数目为0，应当忽略掉，不参与计算
     * @param interval 间隔，分数段之间的默认间隔，如果按照该间隔，无法达到 num 个有数据的分数段，则将间隔减少0.2分，直到达到 num 个分数段
     */
    List<ScoreRangeCourseCO> scoreRangeCourseInfo(Integer num,Integer interval);
}
