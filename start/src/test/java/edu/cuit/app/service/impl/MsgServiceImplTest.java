package edu.cuit.app.service.impl;

import edu.cuit.app.convertor.MsgBizConvertor;
import edu.cuit.app.convertor.course.CourseBizConvertor;
import edu.cuit.app.websocket.WebsocketManager;
import edu.cuit.bc.evaluation.application.port.EvaRecordCountQueryPort;
import edu.cuit.bc.evaluation.application.port.EvaTaskQueryPort;
import edu.cuit.domain.gateway.MsgGateway;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class MsgServiceImplTest {

    @Test
    void shouldConstruct() {
        Executor executor = Runnable::run;
        MsgServiceImpl service = new MsgServiceImpl(
                mock(MsgGateway.class),
                mock(UserQueryGateway.class),
                mock(EvaTaskQueryPort.class),
                mock(EvaRecordCountQueryPort.class),
                mock(WebsocketManager.class),
                mock(MsgBizConvertor.class),
                mock(CourseBizConvertor.class),
                executor
        );
        assertNotNull(service);
    }
}

