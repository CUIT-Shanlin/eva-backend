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
     * @param id 用户 ID 编号
     */
    List<EvaRecordCO> getEvaLogInfo(Integer id);

    /**
     * 获取对于自己进行的评教的记录
     * @param courseId 筛选的该用户教学的课程的id
     */
    List<EvaRecordCO> getEvaLoggingInfo(Integer courseId);
}
