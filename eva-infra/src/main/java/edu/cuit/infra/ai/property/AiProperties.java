package edu.cuit.infra.ai.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai")
@Data
public class AiProperties {

    String siliconApiKey;

    String dashscopeApiKey;

}
