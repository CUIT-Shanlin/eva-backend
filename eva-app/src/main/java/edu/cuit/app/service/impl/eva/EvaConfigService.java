package edu.cuit.app.service.impl.eva;

import edu.cuit.app.convertor.eva.EvaConfigBizConvertor;
import edu.cuit.client.api.eva.IEvaConfigService;
import edu.cuit.client.dto.data.EvaConfig;
import edu.cuit.domain.gateway.eva.EvaConfigGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
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
        evaConfigGateway.updateEvaConfig(config);
    }
}
