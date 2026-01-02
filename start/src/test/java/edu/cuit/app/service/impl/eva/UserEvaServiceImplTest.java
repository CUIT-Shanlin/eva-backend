package edu.cuit.app.service.impl.eva;

import edu.cuit.bc.evaluation.application.usecase.UserEvaQueryUseCase;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class UserEvaServiceImplTest {

    @Test
    void shouldConstruct() {
        UserEvaServiceImpl service = new UserEvaServiceImpl(
                mock(UserEvaQueryUseCase.class),
                mock(UserQueryGateway.class)
        );
        assertNotNull(service);
    }
}
