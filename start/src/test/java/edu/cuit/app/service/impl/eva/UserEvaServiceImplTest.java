package edu.cuit.app.service.impl.eva;

import edu.cuit.bc.iam.application.port.UserBasicQueryPort;
import edu.cuit.bc.evaluation.application.usecase.UserEvaQueryUseCase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class UserEvaServiceImplTest {

    @Test
    void shouldConstruct() {
        UserEvaServiceImpl service = constructService();
        assertNotNull(service);
    }

    /**
     * 构造校验：UserEvaServiceImpl 已完成依赖收敛（Port 版本），因此测试无需再兼容旧构造。
     */
    private static UserEvaServiceImpl constructService() {
        UserEvaQueryUseCase useCase = mock(UserEvaQueryUseCase.class);
        UserBasicQueryPort userBasicQueryPort = mock(UserBasicQueryPort.class);
        return new UserEvaServiceImpl(useCase, userBasicQueryPort);
    }
}
