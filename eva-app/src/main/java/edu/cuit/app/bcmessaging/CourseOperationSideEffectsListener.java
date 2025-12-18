package edu.cuit.app.bcmessaging;

import edu.cuit.bc.messaging.application.event.CourseOperationSideEffectsEvent;
import edu.cuit.bc.messaging.application.usecase.HandleCourseOperationSideEffectsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 课程业务操作副作用监听器：把跨域联动收敛到 bc-messaging。
 */
@Component
@RequiredArgsConstructor
public class CourseOperationSideEffectsListener {
    private final HandleCourseOperationSideEffectsUseCase useCase;

    @EventListener
    public void on(CourseOperationSideEffectsEvent event) {
        useCase.handle(event);
    }
}

