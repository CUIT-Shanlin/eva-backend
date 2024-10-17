package edu.cuit.client.api.eva;

import edu.cuit.client.dto.clientobject.eva.EvaRecordCO;
import edu.cuit.client.dto.clientobject.eva.EvaTaskDetailInfoCO;

import java.util.List;

/**
 * 用户评教相关业务接口
 */
public interface IUserEvaService {

    /**
     * 获取自己的评教记录
     * @param semId 学期id
     * @param keyword 模糊查询的关键字，模糊查询课程名称或教学老师姓名
     */
    List<EvaRecordCO> getEvaLogInfo(Integer semId,String keyword);

    /**
     * 获取对于自己进行的评教的记录
     * @param courseId 筛选的该用户教学的课程的id（负数或null则是 全部
     * @param semId 学期id
     */
    List<EvaRecordCO> getEvaLoggingInfo(Integer courseId,Integer semId);
}
