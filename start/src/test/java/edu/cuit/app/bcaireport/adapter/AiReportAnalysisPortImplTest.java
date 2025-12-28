package edu.cuit.app.bcaireport.adapter;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import edu.cuit.bc.evaluation.application.port.EvaRecordExportQueryPort;
import edu.cuit.client.api.course.IUserCourseService;
import edu.cuit.domain.gateway.eva.EvaConfigGateway;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

class AiReportAnalysisPortImplTest {

    @Test
    void constructor_shouldNotThrow_whenAllDependenciesProvided() {
        IUserCourseService userCourseService = mock(IUserCourseService.class);
        EvaRecordExportQueryPort evaRecordQueryPort = mock(EvaRecordExportQueryPort.class);
        UserQueryGateway userQueryGateway = mock(UserQueryGateway.class);
        EvaConfigGateway evaConfigGateway = mock(EvaConfigGateway.class);

        QwenChatModel qwenMaxChatModel = QwenChatModel.builder()
                .apiKey("test")
                .modelName("qwen-max")
                .build();
        QwenChatModel qwenTurboChatModel = QwenChatModel.builder()
                .apiKey("test")
                .modelName("qwen-turbo")
                .build();
        QwenChatModel deepseekChatModel = QwenChatModel.builder()
                .apiKey("test")
                .modelName("deepseek-v3")
                .build();

        assertDoesNotThrow(() -> new AiReportAnalysisPortImpl(
                userCourseService,
                evaRecordQueryPort,
                userQueryGateway,
                evaConfigGateway,
                qwenMaxChatModel,
                qwenTurboChatModel,
                deepseekChatModel
        ));
    }
}

