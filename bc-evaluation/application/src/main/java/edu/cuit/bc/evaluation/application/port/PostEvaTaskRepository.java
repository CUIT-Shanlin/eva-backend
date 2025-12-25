package edu.cuit.bc.evaluation.application.port;

import edu.cuit.bc.evaluation.application.model.PostEvaTaskCommand;

/**
 * 发布评教任务端口（写侧持久化/外部依赖）。
 */
public interface PostEvaTaskRepository {

    /**
     * 创建评教任务并返回任务 ID。
     *
     * @param command 发布评教任务命令
     * @param maxBeEvaNum 被评教次数上限（来自配置）
     * @return 新任务 ID
     */
    Integer create(PostEvaTaskCommand command, Integer maxBeEvaNum);
}

