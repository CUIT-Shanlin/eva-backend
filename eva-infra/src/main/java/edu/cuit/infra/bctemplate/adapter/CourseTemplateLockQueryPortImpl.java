package edu.cuit.infra.bctemplate.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.template.application.port.CourseTemplateLockQueryPort;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.eva.CourOneEvaTemplateDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormRecordDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.infra.dal.database.mapper.eva.CourOneEvaTemplateMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * bc-template：课程模板锁定查询端口实现（基于现有表结构）。
 */
@Component
@RequiredArgsConstructor
public class CourseTemplateLockQueryPortImpl implements CourseTemplateLockQueryPort {
    private final CourOneEvaTemplateMapper courOneEvaTemplateMapper;
    private final CourInfMapper courInfMapper;
    private final EvaTaskMapper evaTaskMapper;
    private final FormRecordMapper formRecordMapper;

    @Override
    public boolean isLocked(Integer courseId, Integer semesterId) {
        if (courseId == null) {
            return false;
        }
        if (isLockedBySnapshot(courseId, semesterId)) {
            return true;
        }
        return isLockedByRecord(courseId);
    }

    private boolean isLockedBySnapshot(Integer courseId, Integer semesterId) {
        QueryWrapper<CourOneEvaTemplateDO> qw = new QueryWrapper<CourOneEvaTemplateDO>().eq("course_id", courseId);
        if (semesterId != null) {
            qw.eq("semester_id", semesterId);
        }
        Long count = courOneEvaTemplateMapper.selectCount(qw);
        return count != null && count > 0;
    }

    private boolean isLockedByRecord(Integer courseId) {
        List<Integer> courInfIds = courInfMapper.selectList(new QueryWrapper<CourInfDO>().eq("course_id", courseId))
                .stream()
                .map(CourInfDO::getId)
                .toList();
        if (courInfIds.isEmpty()) {
            return false;
        }
        List<Integer> taskIds = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>().in("cour_inf_id", courInfIds))
                .stream()
                .map(EvaTaskDO::getId)
                .toList();
        if (taskIds.isEmpty()) {
            return false;
        }
        Long count = formRecordMapper.selectCount(new QueryWrapper<FormRecordDO>().in("task_id", taskIds));
        return count != null && count > 0;
    }
}

