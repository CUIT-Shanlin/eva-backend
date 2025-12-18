package edu.cuit.bc.messaging.application.usecase;

import edu.cuit.bc.messaging.application.event.CourseOperationSideEffectsEvent;
import edu.cuit.bc.messaging.application.port.CourseBroadcastPort;
import edu.cuit.bc.messaging.application.port.EvaMessageCleanupPort;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HandleCourseOperationSideEffectsUseCaseTest {

    @Test
    void handle_shouldSendToAllWhenValueNull_andSendNormalAndCleanupWhenValueNonEmpty() {
        RecordingBroadcastPort broadcastPort = new RecordingBroadcastPort();
        RecordingCleanupPort cleanupPort = new RecordingCleanupPort();
        HandleCourseOperationSideEffectsUseCase useCase = new HandleCourseOperationSideEffectsUseCase(broadcastPort, cleanupPort);

        Map<String, Map<Integer, Integer>> messageMap = new HashMap<>();
        messageMap.put("全体通知", null);
        Map<Integer, Integer> taskMap = new HashMap<>();
        taskMap.put(10, 1);
        taskMap.put(20, 1);
        messageMap.put("撤回任务通知", taskMap);
        messageMap.put("空任务不通知", Map.of());

        useCase.handle(new CourseOperationSideEffectsEvent(1, messageMap));

        assertEquals(1, broadcastPort.toAll.size());
        assertEquals(1, broadcastPort.normal.size());
        assertEquals(Set.of(10, 20), Set.copyOf(cleanupPort.deletedTaskIds));
    }

    private static class RecordingBroadcastPort implements CourseBroadcastPort {
        private final List<Map<String, Map<Integer, Integer>>> toAll = new ArrayList<>();
        private final List<Map<String, Map<Integer, Integer>>> normal = new ArrayList<>();

        @Override
        public void sendToAll(Map<String, Map<Integer, Integer>> messageMap, Integer operatorUserId) {
            toAll.add(messageMap);
        }

        @Override
        public void sendNormal(Map<String, Map<Integer, Integer>> messageMap, Integer operatorUserId) {
            normal.add(messageMap);
        }
    }

    private static class RecordingCleanupPort implements EvaMessageCleanupPort {
        private final List<Integer> deletedTaskIds = new ArrayList<>();

        @Override
        public void deleteEvaMsg(Integer taskId) {
            deletedTaskIds.add(taskId);
        }
    }
}
