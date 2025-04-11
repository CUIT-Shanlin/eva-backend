package edu.cuit.infra.ai.config;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import edu.cuit.infra.ai.property.AiProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelConfig {

    @Bean
    public QwenChatModel qwenTurboChatModel(AiProperties properties) {
        return QwenChatModel.builder()
                .apiKey(properties.getDashscopeApiKey())
                .modelName("qwen-turbo")
                .temperature(0.2f)
                .build();
    }

    @Bean
    public QwenChatModel qwenMaxChatModel(AiProperties properties) {
        return QwenChatModel.builder()
                .apiKey(properties.getDashscopeApiKey())
                .modelName("qwen-max")
                .build();
    }

    @Bean
    public QwenChatModel deepseekChatModel(AiProperties properties) {
        return QwenChatModel.builder()
                .apiKey(properties.getDashscopeApiKey())
                .modelName("deepseek-v3")
                .build();
    }

}
