package edu.cuit.app.bcaireport.adapter;

import edu.cuit.app.poi.ai.AiReportExporter;
import edu.cuit.bc.aireport.application.port.AiReportDocExportPort;
import edu.cuit.client.bo.ai.AiAnalysisBO;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * AI 报告导出端口适配器（过渡期实现）。
 *
 * <p>保持行为不变：仅原样执行文档生成与二进制写出；异常文案与日志由旧入口保持。</p>
 */
@Component
public class AiReportDocExportPortImpl implements AiReportDocExportPort {
    @Override
    public byte[] exportDocData(AiAnalysisBO analysis) throws IOException {
        XWPFDocument document = new AiReportExporter().generateReport(analysis);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        document.write(outputStream);
        byte[] bytes = outputStream.toByteArray();
        outputStream.close();
        return bytes;
    }
}
