package edu.cuit.infra.gateway.impl.course.support;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.eva.CourOneEvaTemplateDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormRecordDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.infra.dal.database.mapper.eva.CourOneEvaTemplateMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormRecordMapper;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 课程模板锁定校验器。
 *
 * <p>业务规则：只要有人对该课程发生过评教（产生评教记录），则该课程在该学期的模板即锁定，不允许切换。</p>
 *
 * <p>落地策略（从强到弱）：</p>
 * <ol>
 *   <li>优先以 cour_one_eva_template 快照存在作为“已评教”证据（推荐且性能好）。</li>
 *   <li>若快照缺失（历史数据/缺陷导致），则回退到 form_record 反查（保守锁定）。</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class CourseTemplateLockChecker {
    private final CourOneEvaTemplateMapper courOneEvaTemplateMapper;
    private final CourInfMapper courInfMapper;
    private final EvaTaskMapper evaTaskMapper;
    private final FormRecordMapper formRecordMapper;

    public void assertNotLocked(Integer courseId, Integer semesterId) {
        if (courseId == null) {
            return;
        }
        if (isLockedBySnapshot(courseId, semesterId)) {
            throw new UpdateException("课程已评教过，模板已锁定，无法切换");
        }
        if (isLockedByRecord(courseId)) {
            throw new UpdateException("课程已评教过，模板已锁定，无法切换");
        }
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

