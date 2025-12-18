package edu.cuit.bc.messaging.application.usecase;

import edu.cuit.bc.messaging.application.event.CourseTeacherTaskMessagesEvent;
import edu.cuit.bc.messaging.application.port.TeacherTaskMessagePort;

import java.util.Objects;

/**
 * 处理“分配听课/评教老师”后的老师任务消息发送。
 */
public class HandleCourseTeacherTaskMessagesUseCase {
    private final TeacherTaskMessagePort teacherTaskMessagePort;

    public HandleCourseTeacherTaskMessagesUseCase(TeacherTaskMessagePort teacherTaskMessagePort) {
        this.teacherTaskMessagePort = Objects.requireNonNull(teacherTaskMessagePort, "teacherTaskMessagePort");
    }

    public void handle(CourseTeacherTaskMessagesEvent event) {
        if (event == null || event.messageMap() == null) {
            return;
        }
        teacherTaskMessagePort.sendToTeacher(event.messageMap(), event.operatorUserId());
    }
}

