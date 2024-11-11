package edu.cuit.infra.gateway.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import edu.cuit.domain.entity.DynamicConfigEntity;
import edu.cuit.domain.gateway.DynamicConfigGateway;
import edu.cuit.infra.property.StaticConfigProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.File;

@Component
@RequiredArgsConstructor
public class DynamicConfigGatewayImpl implements DynamicConfigGateway {

    private final StaticConfigProperties staticConfigProperties;

    private File evaConfigFile;

    @PostConstruct
    public void initConfig() {
        String configPath = staticConfigProperties.getDirectory();
        File file = new File(configPath);
        if (!file.exists()) file.mkdir();
        evaConfigFile = new File(configPath + File.separatorChar + "eva-config.json");
        if (!evaConfigFile.exists()) {
            File defaultConfig = new File(ResourceUtil.getResource("eva-config.json").getPath());
            FileUtil.copy(defaultConfig,evaConfigFile,false);
        }
    }

    @Override
    public Integer toGetMaxEvaNum() {
        return 0;
    }

    @Override
    public Integer getMinBeEvaNum() {
        return 0;
    }

    @Override
    public Integer getMaxBeEvaNum() {
        return 0;
    }

    @Override
    public DynamicConfigEntity getEvaConfig() {
        return null;
    }

    @Override
    public void updateEvaConfig() {

    }
}
