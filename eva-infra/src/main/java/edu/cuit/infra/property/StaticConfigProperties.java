package edu.cuit.infra.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 静态配置的配置
 */
@ConfigurationProperties(prefix = "eva.config")
@Data
public class StaticConfigProperties {

    // 配置文件存放目录
    private String directory = "/data/eva/config";

}
