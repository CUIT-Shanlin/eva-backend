package edu.cuit.infra.gateway.impl.eva;

import cn.hutool.core.io.FileUtil;
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
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class EvaConfigGatewayImpl implements EvaConfigGateway {

    private final StaticConfigProperties staticConfigProperties;

    private final ApplicationContext applicationContext;

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

            Resource defaultConfigResource = applicationContext.getResource("classpath:eva-config.json");
            try {
                FileUtil.writeFromStream(defaultConfigResource.getInputStream(),evaConfigFile);
            } catch (IOException e) {
                log.error("读取配置失败",e);
                throw new SysException("读取配置失败");
            }
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
        // 兼容：旧客户端/旧页面可能未携带新字段，写入时用当前缓存值兜底，避免把配置写成 null
        if (evaConfig.getHighScoreThreshold() == null) {
            evaConfig.setHighScoreThreshold(configCache.getHighScoreThreshold());
        }

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
            JsonNode minEvaNum = jsonNode.get("minEvaNum");
            if (minEvaNum != null && minEvaNum.isNumber()) {
                configCache.setMinEvaNum(minEvaNum.intValue());
            }
            JsonNode minBeEvaNum = jsonNode.get("minBeEvaNum");
            if (minBeEvaNum != null && minBeEvaNum.isNumber()) {
                configCache.setMinBeEvaNum(minBeEvaNum.intValue());
            }
            JsonNode maxBeEvaNum = jsonNode.get("maxBeEvaNum");
            if (maxBeEvaNum != null && maxBeEvaNum.isNumber()) {
                configCache.setMaxBeEvaNum(maxBeEvaNum.intValue());
            }
            JsonNode highScoreThreshold = jsonNode.get("highScoreThreshold");
            if (highScoreThreshold != null && highScoreThreshold.isNumber()) {
                configCache.setHighScoreThreshold(highScoreThreshold.intValue());
            }
        } catch (IOException e) {
            SysException e1 = new SysException("评教配置文件读取失败");
            log.error("评教配置文件读取失败",e);
            throw e1;
        }
    }
}
