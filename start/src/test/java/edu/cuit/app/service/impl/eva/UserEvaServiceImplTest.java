package edu.cuit.app.service.impl.eva;

import edu.cuit.app.convertor.eva.EvaRecordBizConvertor;
import edu.cuit.bc.evaluation.application.port.EvaRecordScoreQueryPort;
import edu.cuit.bc.evaluation.application.port.EvaRecordUserLogQueryPort;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class UserEvaServiceImplTest {

    @Test
    void shouldConstruct() {
        UserEvaServiceImpl service = new UserEvaServiceImpl(
                mock(EvaRecordUserLogQueryPort.class),
                mock(EvaRecordScoreQueryPort.class),
                mock(EvaRecordBizConvertor.class),
                mock(UserQueryGateway.class)
        );
        assertNotNull(service);
    }
}

