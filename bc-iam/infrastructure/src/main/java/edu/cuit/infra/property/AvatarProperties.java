package edu.cuit.infra.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "eva.avatar")
@Data
public class AvatarProperties {

    // 头像图片存储文件夹路径（只在生产环境生效，开发环境在项目文件夹中的data/avatar）
    private String directory = "/data/eva/avatar";

}
