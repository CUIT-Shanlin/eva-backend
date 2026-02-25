package edu.cuit.bc.evaluation.application.port;

import java.util.List;

/**
 * 评教侧“课程模板锁定判定”最小证据直查端口（读侧）。
 *
 * <p>用于跨 BC 承接模板侧锁定判定链路中对评教表的直连查询（保持行为不变）。当前仅覆盖：</p>
 * <ul>
 *     <li>快照证据：cour_one_eva_template 是否存在（courseId + 可选 semesterId）</li>
 *     <li>记录证据：form_record 是否存在（taskIds in ...）</li>
 * </ul>
 *
 * <p>约束：实现侧不引入缓存/切面副作用；查询条件、结果语义与旧 Mapper selectCount 保持一致；
 * 入参为空或空列表应按 no-op 处理并返回 false。</p>
 */
public interface CourseTemplateLockEvaEvidenceDirectQueryPort {

    /**
     * 是否存在“课程-评教模板快照”记录。
     *
     * @param courseId   课程 ID（必填）
     * @param semesterId 学期 ID（可空；为空表示不限定学期条件）
     * @return 存在返回 true，否则返回 false
     */
    boolean existsSnapshot(Integer courseId, Integer semesterId);

    /**
     * 是否存在“评教表单记录”记录。
     *
     * @param taskIds 评教任务 ID 列表（为空或空列表应视为 no-op）
     * @return 存在返回 true，否则返回 false
     */
    boolean existsFormRecordByTaskIds(List<Integer> taskIds);
}

