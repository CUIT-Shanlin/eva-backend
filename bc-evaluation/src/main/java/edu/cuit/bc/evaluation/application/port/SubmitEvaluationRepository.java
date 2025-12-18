package edu.cuit.bc.evaluation.application.port;

import edu.cuit.bc.evaluation.application.model.FormPropValue;
import edu.cuit.bc.evaluation.application.model.SubmitEvaluationContext;

import java.util.List;

/**
 * 提交评教用例的持久化端口（写侧）。
 *
 * <p>说明：</p>
 * <ul>
 *   <li>读写分离：loadContext 属于写用例的必要读取；saveEvaluation 执行写入与必要的幂等/一致性保护。</li>
 *   <li>实现必须在同一事务中保证：插入评教记录、更新任务状态、创建模板快照（若不存在）。</li>
 * </ul>
 */
public interface SubmitEvaluationRepository {
    SubmitEvaluationContext loadContext(Integer taskId);

    void saveEvaluation(SubmitEvaluationContext context, String textValue, List<FormPropValue> formPropsValues);
}

