package edu.cuit.domain.gateway.eva;

import org.springframework.stereotype.Component;

/**
 * 评教更新相关用户数据接口
 */
@Component
public interface EvaUpdateGateway {
    /**
     * 任意取消一个评教任务
     * @param id 任务id
     */
    Void cancelEvaTaskById(Integer id);

}
