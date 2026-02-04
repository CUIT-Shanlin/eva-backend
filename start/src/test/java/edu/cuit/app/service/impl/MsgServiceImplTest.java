package edu.cuit.app.service.impl;

import edu.cuit.app.convertor.MsgBizConvertor;
import edu.cuit.app.convertor.course.CourseBizConvertor;
import edu.cuit.app.websocket.WebsocketManager;
import edu.cuit.bc.iam.application.port.UserBasicQueryPort;
import edu.cuit.bc.iam.application.port.UserAllUserIdQueryPort;
import edu.cuit.bc.evaluation.application.port.EvaRecordCountQueryPort;
import edu.cuit.bc.evaluation.application.port.EvaTaskInfoQueryPort;
import edu.cuit.domain.gateway.MsgGateway;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class MsgServiceImplTest {

    @Test
    void shouldConstruct() {
        MsgServiceImpl service = constructService();
        assertNotNull(service);
    }

    /**
     * 构造校验：MsgServiceImpl 已完成依赖收敛（Port 版本），因此测试无需再兼容旧构造。
     */
    private static MsgServiceImpl constructService() {
        Executor executor = Runnable::run;
        MsgGateway msgGateway = mock(MsgGateway.class);
        UserBasicQueryPort userBasicQueryPort = mock(UserBasicQueryPort.class);
        UserAllUserIdQueryPort userAllUserIdQueryPort = mock(UserAllUserIdQueryPort.class);
        EvaTaskInfoQueryPort evaTaskInfoQueryPort = mock(EvaTaskInfoQueryPort.class);
        EvaRecordCountQueryPort evaRecordCountQueryPort = mock(EvaRecordCountQueryPort.class);
        WebsocketManager websocketManager = mock(WebsocketManager.class);
        MsgBizConvertor msgBizConvertor = mock(MsgBizConvertor.class);
        CourseBizConvertor courseBizConvertor = mock(CourseBizConvertor.class);
        return new MsgServiceImpl(
                msgGateway,
                userBasicQueryPort,
                userAllUserIdQueryPort,
                evaTaskInfoQueryPort,
                evaRecordCountQueryPort,
                websocketManager,
                msgBizConvertor,
                courseBizConvertor,
                executor
        );
    }
}
