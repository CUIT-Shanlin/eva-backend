package edu.cuit.app.service.impl.eva;

import com.alibaba.cola.exception.BizException;
import edu.cuit.app.convertor.eva.EvaConfigBizConvertor;
import edu.cuit.client.api.eva.IEvaConfigService;
import edu.cuit.client.dto.data.EvaConfig;
import edu.cuit.domain.gateway.eva.EvaConfigGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EvaConfigService implements IEvaConfigService {

    private final EvaConfigGateway evaConfigGateway;

    private final EvaConfigBizConvertor evaConfigBizConvertor;

    @Override
    public EvaConfig getEvaConfig() {
        return evaConfigBizConvertor.toEvaConfig(evaConfigGateway.getEvaConfig());
    }

    @Override
    public void updateEvaConfig(EvaConfig config) {
        if (config.getMinBeEvaNum() > config.getMaxBeEvaNum()) {
            throw new BizException("最大被评教次数应大于等于最小被评教次数");
        }
        evaConfigGateway.updateEvaConfig(config);
    }
}
