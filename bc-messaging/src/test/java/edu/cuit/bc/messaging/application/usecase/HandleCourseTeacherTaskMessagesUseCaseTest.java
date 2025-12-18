package edu.cuit.bc.messaging.application.usecase;

import edu.cuit.bc.messaging.application.event.CourseTeacherTaskMessagesEvent;
import edu.cuit.bc.messaging.application.port.TeacherTaskMessagePort;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HandleCourseTeacherTaskMessagesUseCaseTest {

    @Test
    void handle_shouldDelegateToPort() {
        RecordingPort port = new RecordingPort();
        HandleCourseTeacherTaskMessagesUseCase useCase = new HandleCourseTeacherTaskMessagesUseCase(port);

        Map<String, Map<Integer, Integer>> payload = new HashMap<>();
        payload.put("任务通知", Map.of(10, 200));

        useCase.handle(new CourseTeacherTaskMessagesEvent(1, payload));

        assertEquals(1, port.calls);
        assertEquals(1, port.operatorUserId);
        assertEquals(payload, port.messageMap);
    }

    private static class RecordingPort implements TeacherTaskMessagePort {
        private int calls = 0;
        private Integer operatorUserId;
        private Map<String, Map<Integer, Integer>> messageMap;

        @Override
        public void sendToTeacher(Map<String, Map<Integer, Integer>> messageMap, Integer operatorUserId) {
            this.calls++;
            this.operatorUserId = operatorUserId;
            this.messageMap = messageMap;
        }
    }
}

