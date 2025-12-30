package edu.cuit.app.bcmessaging;

import edu.cuit.bc.messaging.application.event.CourseTeacherTaskMessagesEvent;
import edu.cuit.bc.messaging.application.usecase.HandleCourseTeacherTaskMessagesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 分配听课/评教老师后，向老师发送任务消息的监听器。
 */
@Component
@RequiredArgsConstructor
public class CourseTeacherTaskMessagesListener {
    private final HandleCourseTeacherTaskMessagesUseCase useCase;

    @EventListener
    public void on(CourseTeacherTaskMessagesEvent event) {
        useCase.handle(event);
    }
}
