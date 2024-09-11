package edu.cuit.client.api.eva;

import edu.cuit.client.dto.clientobject.eva.EvaRecordCO;
import edu.cuit.client.dto.clientobject.eva.EvaTaskDetailInfoCO;

/**
 * 用户评教相关业务接口
 */
public interface IUserEvaService {

    /**
     * 获取单个用户的待办评教任务，主要用于移动端
     * @param id 用户 ID 编号
     */
    EvaTaskDetailInfoCO evaUnfinishedTaskInfo(Integer id);

    /**
     * 获取单个用户的评教记录
     * @param id 用户 ID 编号
     */
    EvaRecordCO oneEvaLogInfo(Integer id);

    /**
     * 获取对于单个用户进行的评教记录
     * @param userId 用户 ID 编号
     * @param courseId 筛选的该用户教学的课程的id
     */
    EvaRecordCO oneEvaLogInfo(Integer userId, Integer courseId);

}
