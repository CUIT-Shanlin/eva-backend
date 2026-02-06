package edu.cuit.domain.gateway;

import org.springframework.stereotype.Component;

@Component
public interface DynamicConfigGateway {
    /**
     * 获取最大评教上限
     * */
    public Integer toGetMaxEvaNum();

}
