package edu.cuit.app.service.impl.ai;

import cn.dev33.satoken.stp.StpUtil;
import edu.cuit.bc.aireport.application.usecase.AnalyseAiReportUseCase;
import edu.cuit.bc.aireport.application.usecase.ExportAiReportDocByUsernameUseCase;
import edu.cuit.app.aop.CheckSemId;
import edu.cuit.client.api.ai.IAiCourseAnalysisService;
import edu.cuit.client.bo.ai.AiAnalysisBO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCourseAnalysisService implements IAiCourseAnalysisService {

    private final AnalyseAiReportUseCase analyseAiReportUseCase;
    private final ExportAiReportDocByUsernameUseCase exportAiReportDocByUsernameUseCase;

    @Override
    @CheckSemId
    public AiAnalysisBO analysis(Integer semId, Integer teacherId) {
        return analyseAiReportUseCase.analysis(semId, teacherId);
    }

    @Override
    public byte[] exportDocData(Integer semId) {
        return exportAiReportDocByUsernameUseCase.exportDocData(semId, ((String) StpUtil.getLoginId()));
    }
}
