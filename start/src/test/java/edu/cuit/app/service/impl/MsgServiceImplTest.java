package edu.cuit.app.service.impl;

import edu.cuit.app.convertor.MsgBizConvertor;
import edu.cuit.app.convertor.course.CourseBizConvertor;
import edu.cuit.app.websocket.WebsocketManager;
import edu.cuit.bc.iam.application.port.UserBasicQueryPort;
import edu.cuit.bc.evaluation.application.port.EvaRecordCountQueryPort;
import edu.cuit.bc.evaluation.application.port.EvaTaskInfoQueryPort;
import edu.cuit.domain.gateway.MsgGateway;
import edu.cuit.domain.gateway.user.UserQueryGateway;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
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
     * 测试过渡：兼容“旧构造（UserQueryGateway）”与“新构造（Port）”，
     * 以支持后续对 MsgServiceImpl 的依赖收敛（保持行为不变）。
     */
    private static MsgServiceImpl constructService() {
        Executor executor = Runnable::run;
        MsgGateway msgGateway = mock(MsgGateway.class);
        UserBasicQueryPort userBasicQueryPort = mock(UserBasicQueryPort.class);
        UserQueryGateway userQueryGateway = mock(UserQueryGateway.class);
        EvaTaskInfoQueryPort evaTaskInfoQueryPort = mock(EvaTaskInfoQueryPort.class);
        EvaRecordCountQueryPort evaRecordCountQueryPort = mock(EvaRecordCountQueryPort.class);
        WebsocketManager websocketManager = mock(WebsocketManager.class);
        MsgBizConvertor msgBizConvertor = mock(MsgBizConvertor.class);
        CourseBizConvertor courseBizConvertor = mock(CourseBizConvertor.class);

        // 先尝试新签名（Port 版本），如果暂未落地则回退旧签名
        try {
            Class<?> userAllUserIdQueryPortClass = Class.forName("edu.cuit.bc.iam.application.port.UserAllUserIdQueryPort");
            Object userAllUserIdQueryPort = mock((Class<?>) userAllUserIdQueryPortClass);
            Constructor<MsgServiceImpl> constructor = MsgServiceImpl.class.getDeclaredConstructor(
                    MsgGateway.class,
                    UserBasicQueryPort.class,
                    userAllUserIdQueryPortClass,
                    EvaTaskInfoQueryPort.class,
                    EvaRecordCountQueryPort.class,
                    WebsocketManager.class,
                    MsgBizConvertor.class,
                    CourseBizConvertor.class,
                    Executor.class
            );
            return constructor.newInstance(
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
        } catch (ClassNotFoundException ignored) {
            // Port 尚未引入，走旧构造
        } catch (NoSuchMethodException ignored) {
            // 构造签名暂未切换，走旧构造
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            Constructor<MsgServiceImpl> constructor = MsgServiceImpl.class.getDeclaredConstructor(
                    MsgGateway.class,
                    UserQueryGateway.class,
                    EvaTaskInfoQueryPort.class,
                    EvaRecordCountQueryPort.class,
                    WebsocketManager.class,
                    MsgBizConvertor.class,
                    CourseBizConvertor.class,
                    Executor.class
            );
            return constructor.newInstance(
                    msgGateway,
                    userQueryGateway,
                    evaTaskInfoQueryPort,
                    evaRecordCountQueryPort,
                    websocketManager,
                    msgBizConvertor,
                    courseBizConvertor,
                    executor
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
