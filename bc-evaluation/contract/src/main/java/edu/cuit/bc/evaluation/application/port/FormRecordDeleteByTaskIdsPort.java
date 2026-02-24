package edu.cuit.bc.evaluation.application.port;

import java.util.List;

/**
 * 评教记录“按 taskIds 删除”端口（写侧）。
 *
 * <p>用于跨 BC 在课程删课等链路中，按 taskIds 删除评教记录（form_record）。</p>
 *
 * <p>约束：实现侧不引入缓存/切面副作用；入参为空或空列表应为 no-op。</p>
 */
public interface FormRecordDeleteByTaskIdsPort {

    void deleteByTaskIds(List<Integer> taskIds);
}

