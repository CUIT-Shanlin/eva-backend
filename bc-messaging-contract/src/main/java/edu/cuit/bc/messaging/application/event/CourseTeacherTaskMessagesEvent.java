package edu.cuit.bc.messaging.application.event;

import java.util.Map;

/**
 * 分配听课/评教老师后，需要给“被分配的老师”发送任务消息的事件。
 *
 * <p>说明：当前处于渐进式重构阶段，事件载荷沿用旧系统的消息模型，以保证行为不变。</p>
 */
public record CourseTeacherTaskMessagesEvent(
        Integer operatorUserId,
        Map<String, Map<Integer, Integer>> messageMap
) { }

