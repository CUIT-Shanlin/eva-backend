package edu.cuit.domain.gateway.eva;

import edu.cuit.domain.entity.eva.EvaTaskEntity;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * 评教删除相关用户数据接口
 */
@Component
public interface EvaDeleteGateway {
    /**
     * 删除一条评教记录/批量删除评教记录
     * @param ids 评教记录id数组
     */
    Void deleteEvaRecord(List<Integer> ids);

    /**
     * 删除评教模板/批量删除模板
     * @param ids 评教模板id数组
     */
    Void deleteEvaTemplate(List<Integer> ids);

    /**
     * 传老师id删除所有对应评教任务
     * @param teacherId 老师id
     */
    List<Integer> deleteAllTaskByTea(Integer teacherId);
}
