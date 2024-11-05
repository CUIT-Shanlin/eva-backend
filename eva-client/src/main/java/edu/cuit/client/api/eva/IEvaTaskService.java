package edu.cuit.client.api.eva;

import edu.cuit.client.dto.clientobject.PaginationQueryResultCO;
import edu.cuit.client.dto.clientobject.eva.AddTaskCO;
import edu.cuit.client.dto.clientobject.eva.EvaInfoCO;
import edu.cuit.client.dto.clientobject.eva.EvaTaskBaseInfoCO;
import edu.cuit.client.dto.clientobject.eva.EvaTaskDetailInfoCO;
import edu.cuit.client.dto.cmd.eva.NewEvaTaskCmd;
import edu.cuit.client.dto.query.PagingQuery;
import edu.cuit.client.dto.query.condition.EvaTaskConditionalQuery;

import java.util.List;

/**
 * 评教任务相关业务接口
 */
public interface IEvaTaskService {

    //查询

    /**
     *分页获取未完成的评教任务+条件查询
     * @param semId 学期id
     * @param query 查询dto
     */
    PaginationQueryResultCO<EvaTaskBaseInfoCO> pageEvaUnfinishedTask(Integer semId, PagingQuery<EvaTaskConditionalQuery> query);
    /**
     * 获取自己的所有待办评教任务
     * @param semId 学期id
     * @param keyword 模糊查询的关键字，模糊查询课程名称或教学老师姓名
     */
    List<EvaTaskDetailInfoCO> evaSelfTaskInfo(Integer semId, String keyword);
    /**
     * 获取一个评教任务的详细信息
     * @param id 任务id
     */
    EvaTaskDetailInfoCO oneEvaTaskInfo(Integer id);
    //修改

    /**
     * 发起评教任务
     *@param newEvaTaskCmd 增加评教dto
     */
    Void postEvaTask(NewEvaTaskCmd newEvaTaskCmd);

    /**
     * 任意取消一个评教任务
     * @param id 课程id
     */

    Void cancelEvaTask(Integer id);
    /**
     * 取消一个自己的评教任务，后端需要检测是不是自己的评教任务，
     * @param id 任务id
     */
    Void cancelMyEvaTask(Integer id);

    /**
     * 传老师id删除所有对应评教任务
     * @param teacherId 老师id
     */
    Void deleteAllTaskByTea(Integer teacherId);
}
