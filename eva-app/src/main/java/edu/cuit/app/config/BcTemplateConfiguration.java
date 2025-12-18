package edu.cuit.app.config;

import edu.cuit.bc.template.application.CourseTemplateLockService;
import edu.cuit.bc.template.application.port.CourseTemplateLockQueryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * bc-template 的组合根（单体阶段由 eva-app 负责装配）。
 */
@Configuration
public class BcTemplateConfiguration {
    @Bean
    public CourseTemplateLockService courseTemplateLockService(CourseTemplateLockQueryPort queryPort) {
        return new CourseTemplateLockService(queryPort);
    }
}

