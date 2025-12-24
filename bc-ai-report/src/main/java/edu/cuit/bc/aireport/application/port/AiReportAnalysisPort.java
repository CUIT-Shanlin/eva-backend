package edu.cuit.bc.aireport.application.port;

import edu.cuit.client.bo.ai.AiAnalysisBO;

/**
 * AI 报告分析端口（过渡期）。
 *
 * <p>保持行为不变：端口仅用于隔离依赖，具体分析逻辑在端口适配器中原样执行。</p>
 */
public interface AiReportAnalysisPort {
    AiAnalysisBO analysis(Integer semId, Integer teacherId);
}
