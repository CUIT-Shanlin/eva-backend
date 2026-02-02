package edu.cuit.app.service.impl.eva;

import edu.cuit.bc.iam.application.port.UserBasicQueryPort;
import edu.cuit.bc.evaluation.application.usecase.UserEvaQueryUseCase;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class UserEvaServiceImplTest {

    @Test
    void shouldConstruct() {
        UserEvaServiceImpl service = constructService();
        assertNotNull(service);
    }

    /**
     * 测试过渡：兼容“旧构造（UserQueryGateway）”与“新构造（UserBasicQueryPort）”，
     * 以支持后续对 UserEvaServiceImpl 的依赖收敛（保持行为不变）。
     */
    private static UserEvaServiceImpl constructService() {
        UserEvaQueryUseCase useCase = mock(UserEvaQueryUseCase.class);
        UserBasicQueryPort userBasicQueryPort = mock(UserBasicQueryPort.class);
        UserQueryGateway userQueryGateway = mock(UserQueryGateway.class);

        try {
            Constructor<UserEvaServiceImpl> constructor =
                    UserEvaServiceImpl.class.getDeclaredConstructor(UserEvaQueryUseCase.class, UserBasicQueryPort.class);
            return constructor.newInstance(useCase, userBasicQueryPort);
        } catch (NoSuchMethodException ignored) {
            // fallback to legacy signature
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            Constructor<UserEvaServiceImpl> constructor =
                    UserEvaServiceImpl.class.getDeclaredConstructor(UserEvaQueryUseCase.class, UserQueryGateway.class);
            return constructor.newInstance(useCase, userQueryGateway);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
