package edu.cuit.bc.evaluation.application.port;

import java.util.Optional;

/**
 * 评教模板“按任务获取模板”等聚合查询端口（读侧）。
 *
 * <p>仅用于接口细分与依赖收敛，不改任何业务语义。</p>
 */
public interface EvaTemplateTaskTemplateQueryPort {
    Optional<String> getTaskTemplate(Integer taskId, Integer semId);
}

