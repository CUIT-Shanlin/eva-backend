package edu.cuit.domain.gateway.eva;

import edu.cuit.client.dto.clientobject.eva.*;
import edu.cuit.client.dto.query.condition.EvaLogConditionalQuery;
import edu.cuit.client.dto.query.condition.EvaTaskConditionalQuery;
import edu.cuit.client.dto.query.condition.GenericConditionalQuery;
import edu.cuit.domain.entity.eva.EvaRecordEntity;
import edu.cuit.domain.entity.eva.EvaTaskEntity;
import edu.cuit.domain.entity.eva.EvaTemplateEntity;
import org.springframework.stereotype.Component;

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
     * @param evaLogConditionalQuery 查询参数
     * @return List<EvaRecordEntity>
     */
    List<EvaRecordEntity> pageEvaRecord(Integer semId, EvaLogConditionalQuery evaLogConditionalQuery);
    /**
     *分页获取未完成的评教任务+条件查询
     * @param semId 学期id
     * @param evaTaskConditionalQuery 查询参数
     * @param evaTaskConditionalQuery 查询参数
     * @return List<EvaTaskEntity>
     */
    List<EvaTaskEntity> pageEvaUnfinishedTask(Integer semId, EvaTaskConditionalQuery evaTaskConditionalQuery);
    /**
     *分页获取评教模板信息
     * @param semId 学期id
     * @param genericConditionalQuery 查询参数
     * @return List<EvaTemplateEntity>
     */
    List<EvaTemplateEntity> pageEvaTemplate(Integer semId, GenericConditionalQuery genericConditionalQuery);

    /**
     * 获取单个用户的待办评教任务，主要用于移动端
     * @param id 用户 ID 编号
     * @return List<EvaTaskEntity>
     */
    List<EvaTaskEntity> evaUnfinishedTaskInfo(Integer id);
    /**
     * 获取单个用户的评教记录
     * @param id 用户 ID 编号
     * @return List<EvaRecordEntity>
     */
    List<EvaRecordEntity> oneEvaLogInfo(Integer id);
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
}
