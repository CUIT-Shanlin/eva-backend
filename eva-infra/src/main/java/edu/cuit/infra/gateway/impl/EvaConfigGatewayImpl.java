package edu.cuit.infra.gateway.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.cola.exception.SysException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cuit.client.dto.data.EvaConfig;
import edu.cuit.domain.entity.eva.EvaConfigEntity;
import edu.cuit.domain.gateway.eva.EvaConfigGateway;
import edu.cuit.infra.property.StaticConfigProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class EvaConfigGatewayImpl implements EvaConfigGateway {

    private final StaticConfigProperties staticConfigProperties;

    private final ObjectMapper objectMapper;

    private File evaConfigFile;
    private EvaConfigEntity configCache = null;

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
        readConfig();
    }

    @Override
    public Integer getMaxBeEvaNum() {
        return configCache.getMaxBeEvaNum();
    }

    @Override
    public Integer getMinBeEvaNum() {
        return configCache.getMinBeEvaNum();
    }

    @Override
    public Integer getMinEvaNum() {
        return configCache.getMinEvaNum();
    }

    @Override
    public EvaConfigEntity getEvaConfig() {
        return configCache.clone();
    }

    @Override
    public void updateEvaConfig(EvaConfig evaConfig) {
        writeConfig(evaConfig);
    }

    private void writeConfig(EvaConfig evaConfig) {
        BufferedOutputStream outputStream = FileUtil.getOutputStream(evaConfigFile);
        try {
            objectMapper.writeValue(outputStream,evaConfig);
        } catch (IOException e) {
            SysException e1 = new SysException("写入评教配置文件失败");
            log.error("写入评教配置文件失败",e);
            throw e1;
        }
        readConfig();
    }

    private void readConfig() {
        BufferedInputStream inputStream = FileUtil.getInputStream(evaConfigFile);
        try {
            JsonNode jsonNode = objectMapper.readTree(inputStream);
            configCache = SpringUtil.getBean(EvaConfigEntity.class);
            configCache.setMinEvaNum(jsonNode.get("minEvaNum").intValue());
            configCache.setMinBeEvaNum(jsonNode.get("minBeEvaNum").intValue());
            configCache.setMaxBeEvaNum(jsonNode.get("maxBeEvaNum").intValue());
        } catch (IOException e) {
            SysException e1 = new SysException("评教配置文件读取失败");
            log.error("评教配置文件读取失败",e);
            throw e1;
        }
    }
}
