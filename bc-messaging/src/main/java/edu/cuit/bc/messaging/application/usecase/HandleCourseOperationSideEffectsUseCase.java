package edu.cuit.bc.messaging.application.usecase;

import edu.cuit.bc.messaging.application.event.CourseOperationSideEffectsEvent;
import edu.cuit.bc.messaging.application.port.CourseBroadcastPort;
import edu.cuit.bc.messaging.application.port.EvaMessageCleanupPort;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 处理课程业务操作的跨域副作用（消息通知、撤回评教消息等）。
 */
public class HandleCourseOperationSideEffectsUseCase {
    private final CourseBroadcastPort courseBroadcastPort;
    private final EvaMessageCleanupPort evaMessageCleanupPort;

    public HandleCourseOperationSideEffectsUseCase(
            CourseBroadcastPort courseBroadcastPort,
            EvaMessageCleanupPort evaMessageCleanupPort
    ) {
        this.courseBroadcastPort = Objects.requireNonNull(courseBroadcastPort, "courseBroadcastPort");
        this.evaMessageCleanupPort = Objects.requireNonNull(evaMessageCleanupPort, "evaMessageCleanupPort");
    }

    public void handle(CourseOperationSideEffectsEvent event) {
        if (event == null || event.messageMap() == null) {
            return;
        }
        Integer operatorUserId = event.operatorUserId();

        for (Map.Entry<String, Map<Integer, Integer>> entry : event.messageMap().entrySet()) {
            Map<String, Map<Integer, Integer>> single = new HashMap<>();
            single.put(entry.getKey(), entry.getValue());

            if (entry.getValue() == null) {
                courseBroadcastPort.sendToAll(single, operatorUserId);
                continue;
            }
            if (!entry.getValue().isEmpty()) {
                courseBroadcastPort.sendNormal(single, operatorUserId);
                entry.getValue().forEach((taskId, ignored) -> evaMessageCleanupPort.deleteEvaMsg(taskId));
            }
        }
    }
}

