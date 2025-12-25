package edu.cuit.client.api.eva;

import edu.cuit.client.dto.data.EvaConfig;

/**
 * 评教动态配置接口
 */
public interface IEvaConfigService {

    /**
     * 获取评教配置
     */
    EvaConfig getEvaConfig();

    /**
     * 修改评教配置
     */
    void updateEvaConfig(EvaConfig config);

}
