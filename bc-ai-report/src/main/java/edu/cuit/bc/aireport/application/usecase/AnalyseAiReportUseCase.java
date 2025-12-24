package edu.cuit.bc.aireport.application.usecase;

import edu.cuit.bc.aireport.application.port.AiReportAnalysisPort;
import edu.cuit.client.bo.ai.AiAnalysisBO;

import java.util.Objects;

/**
 * AI 报告分析用例（写模型入口：生成 AI 分析报告数据）。
 *
 * <p>保持行为不变：用例仅做编排与依赖隔离，具体分析逻辑在端口适配器中原样执行。</p>
 */
public class AnalyseAiReportUseCase {
    private final AiReportAnalysisPort analysisPort;

    public AnalyseAiReportUseCase(AiReportAnalysisPort analysisPort) {
        this.analysisPort = Objects.requireNonNull(analysisPort, "analysisPort");
    }

    public AiAnalysisBO analysis(Integer semId, Integer teacherId) {
        return analysisPort.analysis(semId, teacherId);
    }
}
