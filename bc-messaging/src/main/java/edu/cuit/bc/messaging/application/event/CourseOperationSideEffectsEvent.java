package edu.cuit.bc.messaging.application.event;

import java.util.Map;

/**
 * 课程业务操作产生的副作用事件（用于跨 BC 联动：消息通知、撤回任务等）。
 *
 * <p>说明：当前处于渐进式重构阶段，事件载荷沿用旧系统的消息模型，以保证行为不变。</p>
 */
public record CourseOperationSideEffectsEvent(
        Integer operatorUserId,
        Map<String, Map<Integer, Integer>> messageMap
) { }

