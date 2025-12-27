package edu.cuit.bc.aireport.application.usecase;

import edu.cuit.bc.aireport.application.port.AiReportDocExportPort;
import edu.cuit.client.bo.ai.AiAnalysisBO;

import java.io.IOException;
import java.util.Objects;

/**
 * AI 报告导出用例（写模型入口：生成 Word 文档二进制）。
 *
 * <p>保持行为不变：用例仅做编排与依赖隔离，具体导出逻辑在端口适配器中原样执行。</p>
 */
public class ExportAiReportDocUseCase {
    private final AiReportDocExportPort exportPort;

    public ExportAiReportDocUseCase(AiReportDocExportPort exportPort) {
        this.exportPort = Objects.requireNonNull(exportPort, "exportPort");
    }

    public byte[] exportDocData(AiAnalysisBO analysis) throws IOException {
        return exportPort.exportDocData(analysis);
    }
}
