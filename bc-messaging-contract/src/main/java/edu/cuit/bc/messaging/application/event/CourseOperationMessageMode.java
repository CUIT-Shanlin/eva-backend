package edu.cuit.bc.messaging.application.event;

/**
 * 课程业务操作消息发送模式（用于在“保持行为不变”的前提下，兼容不同历史消息格式）。
 *
 * <p>当前系统存在两类“消息模型”：</p>
 * <ul>
 *     <li>{@link #NORMAL}：消息本身不需要携带 taskId（历史上对应 MsgResult.toNormalMsg，taskId 固定为 -1）。</li>
 *     <li>{@link #TASK_LINKED}：消息需要携带 taskId（历史上对应 MsgResult.toSendMsg，taskId = map 的 key）。</li>
 * </ul>
 *
 * <p>说明：此枚举仅用于渐进式重构阶段的过渡，避免大范围改动旧系统的消息模型。</p>
 */
public enum CourseOperationMessageMode {
    NORMAL,
    TASK_LINKED
}

