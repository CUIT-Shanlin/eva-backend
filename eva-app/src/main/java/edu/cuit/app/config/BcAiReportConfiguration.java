package edu.cuit.app.config;

import org.springframework.context.annotation.Configuration;

/**
 * bc-ai-report 的组合根（单体阶段由 eva-app 负责装配）。
 *
 * <p>提交点 A：仅创建 Maven 模块落点与 wiring，不迁移业务语义。</p>
 */
@Configuration
public class BcAiReportConfiguration {
}

