package edu.cuit.bc.aireport.application.port;

import edu.cuit.client.bo.ai.AiAnalysisBO;

import java.io.IOException;

/**
 * AI 报告导出端口（Word 文档二进制生成）。
 *
 * <p>保持行为不变：导出异常的文案与日志仍由旧入口（委托壳）保持。</p>
 */
public interface AiReportDocExportPort {
    byte[] exportDocData(AiAnalysisBO analysis) throws IOException;
}
