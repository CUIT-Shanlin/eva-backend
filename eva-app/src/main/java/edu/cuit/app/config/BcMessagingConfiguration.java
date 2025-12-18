package edu.cuit.app.config;

import edu.cuit.bc.messaging.application.port.CourseBroadcastPort;
import edu.cuit.bc.messaging.application.port.EvaMessageCleanupPort;
import edu.cuit.bc.messaging.application.port.TeacherTaskMessagePort;
import edu.cuit.bc.messaging.application.usecase.HandleCourseOperationSideEffectsUseCase;
import edu.cuit.bc.messaging.application.usecase.HandleCourseTeacherTaskMessagesUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * bc-messaging 的组合根（单体阶段由 eva-app 负责装配）。
 */
@Configuration
public class BcMessagingConfiguration {
    @Bean
    public HandleCourseOperationSideEffectsUseCase handleCourseOperationSideEffectsUseCase(
            CourseBroadcastPort courseBroadcastPort,
            EvaMessageCleanupPort evaMessageCleanupPort
    ) {
        return new HandleCourseOperationSideEffectsUseCase(courseBroadcastPort, evaMessageCleanupPort);
    }

    @Bean
    public HandleCourseTeacherTaskMessagesUseCase handleCourseTeacherTaskMessagesUseCase(
            TeacherTaskMessagePort teacherTaskMessagePort
    ) {
        return new HandleCourseTeacherTaskMessagesUseCase(teacherTaskMessagePort);
    }
}
