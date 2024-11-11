package edu.cuit.domain.gateway;

import edu.cuit.domain.entity.DynamicConfigEntity;
import org.springframework.stereotype.Component;

/**
 * 动态配置相关数据门面
 */
@Component
public interface DynamicConfigGateway {

    /**
     * 获取最大评教上限
     * */
    Integer toGetMaxEvaNum();

    /**
     * 获取最小被评教次数
     */
    Integer getMinBeEvaNum();

    /**
     * 获取最大被评教次数
     */
    Integer getMaxBeEvaNum();

    /**
     * 获取评教配置
     */
    DynamicConfigEntity getEvaConfig();

    /**
     * 修改评教配置
     */
    void updateEvaConfig();
}
