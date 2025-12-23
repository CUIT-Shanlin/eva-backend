package edu.cuit.bc.aireport.application.usecase;

import com.alibaba.cola.exception.SysException;
import edu.cuit.client.api.ai.IAiCourseAnalysisService;
import edu.cuit.client.bo.ai.AiAnalysisBO;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * AI 报告导出用例（按用户名解析 userId + 触发分析 + 导出 Word 二进制）。
 *
 * <p>保持行为不变：异常文案、日志文案与触发顺序保持不变。</p>
 */
public class ExportAiReportDocByUsernameUseCase {
    private static final Logger log = LoggerFactory.getLogger("edu.cuit.app.service.impl.ai.AiCourseAnalysisService");

    private final UserQueryGateway userQueryGateway;
    private final IAiCourseAnalysisService aiCourseAnalysisService;
    private final ExportAiReportDocUseCase exportAiReportDocUseCase;

    public ExportAiReportDocByUsernameUseCase(
            UserQueryGateway userQueryGateway,
            IAiCourseAnalysisService aiCourseAnalysisService,
            ExportAiReportDocUseCase exportAiReportDocUseCase
    ) {
        this.userQueryGateway = Objects.requireNonNull(userQueryGateway, "userQueryGateway");
        this.aiCourseAnalysisService = Objects.requireNonNull(aiCourseAnalysisService, "aiCourseAnalysisService");
        this.exportAiReportDocUseCase = Objects.requireNonNull(exportAiReportDocUseCase, "exportAiReportDocUseCase");
    }

    public byte[] exportDocData(Integer semId, String username) {
        Integer userId = userQueryGateway.findIdByUsername(username)
                .orElseThrow(() -> {
                    SysException e = new SysException("用户数据查找失败，请联系管理员");
                    log.error("系统异常", e);
                    return e;
                });

        AiAnalysisBO analysis = aiCourseAnalysisService.analysis(semId, userId);
        try {
            return exportAiReportDocUseCase.exportDocData(analysis);
        } catch (IOException e) {
            log.error("AI报告导出失败", e);
            throw new SysException("报告导出失败，请联系管理员");
        }
    }
}
