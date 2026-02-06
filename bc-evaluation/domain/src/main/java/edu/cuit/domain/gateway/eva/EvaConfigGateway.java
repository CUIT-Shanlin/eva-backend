package edu.cuit.domain.gateway.eva;

import edu.cuit.client.dto.data.EvaConfig;
import edu.cuit.domain.entity.eva.EvaConfigEntity;
import org.springframework.stereotype.Component;

/**
 * 动态配置相关数据门面
 */
@Component
public interface EvaConfigGateway {

    /**
     * 获取最大被评教额次数
     * */
    Integer getMaxBeEvaNum();

    /**
     * 获取最小被评教次数
     */
    Integer getMinBeEvaNum();

    /**
     * 获取最小评教次数
     */
    Integer getMinEvaNum();

    /**
     * 获取评教配置
     */
    EvaConfigEntity getEvaConfig();

    /**
     * 修改评教配置
     */
    void updateEvaConfig(EvaConfig evaConfig);
}
