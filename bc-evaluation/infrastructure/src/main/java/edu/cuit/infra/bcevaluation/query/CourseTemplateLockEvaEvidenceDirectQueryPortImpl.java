package edu.cuit.infra.bcevaluation.query;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.evaluation.application.port.CourseTemplateLockEvaEvidenceDirectQueryPort;
import edu.cuit.infra.dal.database.dataobject.eva.CourOneEvaTemplateDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormRecordDO;
import edu.cuit.infra.dal.database.mapper.eva.CourOneEvaTemplateMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormRecordMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 模板锁定判定证据直查端口适配器（保持行为不变）。
 *
 * <p>约束：不引入缓存/切面副作用；查询条件与旧 Mapper selectCount 调用保持一致；入参为空/空列表为 no-op。</p>
 */
@Component
@RequiredArgsConstructor
public class CourseTemplateLockEvaEvidenceDirectQueryPortImpl
        implements CourseTemplateLockEvaEvidenceDirectQueryPort {

    private final CourOneEvaTemplateMapper courOneEvaTemplateMapper;
    private final FormRecordMapper formRecordMapper;

    @Override
    public boolean existsSnapshot(Integer courseId, Integer semesterId) {
        if (courseId == null) {
            return false;
        }

        QueryWrapper<CourOneEvaTemplateDO> qw = new QueryWrapper<CourOneEvaTemplateDO>()
                .eq("course_id", courseId);
        if (semesterId != null) {
            qw.eq("semester_id", semesterId);
        }

        Long count = courOneEvaTemplateMapper.selectCount(qw);
        return count != null && count > 0;
    }

    @Override
    public boolean existsFormRecordByTaskIds(List<Integer> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return false;
        }

        Long count = formRecordMapper.selectCount(new QueryWrapper<FormRecordDO>()
                .in("task_id", taskIds));
        return count != null && count > 0;
    }
}

