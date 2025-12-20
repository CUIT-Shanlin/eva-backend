package edu.cuit.bc.messaging.application.port;

/**
 * 消息删除端口（写侧持久化/外部依赖）。
 */
public interface MessageDeletionPort {

    /**
     * 按任务删除消息（用于评教消息清理）。
     *
     * @param taskId 任务 ID
     * @param type   消息类型；若为 null 或 <0 则不作为筛选条件
     */
    void deleteByTask(Integer taskId, Integer type);

    /**
     * 删除用户自身某类消息（按 mode/type 可选过滤）。
     *
     * @param userId 接收者 ID
     * @param mode   消息模式；若为 null 或 <0 则不作为筛选条件
     * @param type   消息类型；若为 null 或 <0 则不作为筛选条件
     */
    void deleteUserMessages(Integer userId, Integer mode, Integer type);
}

