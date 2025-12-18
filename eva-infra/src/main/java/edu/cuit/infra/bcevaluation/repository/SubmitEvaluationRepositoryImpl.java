package edu.cuit.infra.bcevaluation.repository;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.evaluation.application.model.FormPropValue;
import edu.cuit.bc.evaluation.application.model.SubmitEvaluationContext;
import edu.cuit.bc.evaluation.application.port.SubmitEvaluationRepository;
import edu.cuit.bc.evaluation.domain.SubmitEvaluationException;
import edu.cuit.bc.evaluation.domain.TaskStatus;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.eva.CourOneEvaTemplateDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormRecordDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormTemplateDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.eva.CourOneEvaTemplateMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormRecordMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormTemplateMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.infra.enums.cache.EvaCacheConstants;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * bc-evaluation：提交评教写侧持久化实现（基于现有表结构与缓存体系）。
 */
@Component
@RequiredArgsConstructor
public class SubmitEvaluationRepositoryImpl implements SubmitEvaluationRepository {
    private final EvaTaskMapper evaTaskMapper;
    private final FormRecordMapper formRecordMapper;
    private final CourInfMapper courInfMapper;
    private final CourseMapper courseMapper;
    private final FormTemplateMapper formTemplateMapper;
    private final CourOneEvaTemplateMapper courOneEvaTemplateMapper;
    private final SysUserMapper sysUserMapper;
    private final EvaCacheConstants evaCacheConstants;
    private final LocalCacheManager localCacheManager;

    @Override
    public SubmitEvaluationContext loadContext(Integer taskId) {
        EvaTaskDO task = evaTaskMapper.selectById(taskId);
        if (task == null) {
            return null;
        }

        SysUserDO evaluator = sysUserMapper.selectById(task.getTeacherId());
        CourInfDO courInf = task.getCourInfId() == null ? null : courInfMapper.selectById(task.getCourInfId());
        CourseDO course = courInf == null ? null : courseMapper.selectById(courInf.getCourseId());
        FormTemplateDO template = course == null ? null : formTemplateMapper.selectById(course.getTemplateId());

        return new SubmitEvaluationContext(
                taskId,
                task.getTeacherId(),
                evaluator == null ? null : evaluator.getName(),
                task.getCourInfId(),
                courInf == null ? null : courInf.getCourseId(),
                course == null ? null : course.getSemesterId(),
                course == null ? null : course.getTemplateId(),
                template == null ? null : template.getName(),
                template == null ? null : template.getDescription(),
                template == null ? null : template.getProps(),
                TaskStatus.fromCode(task.getStatus())
        );
    }

    @Override
    @Transactional
    public void saveEvaluation(SubmitEvaluationContext context, String textValue, List<FormPropValue> formPropsValues) {
        if (context == null || context.taskId() == null) {
            throw new SubmitEvaluationException("评教任务不能为空");
        }

        EvaTaskDO task = evaTaskMapper.selectById(context.taskId());
        if (task == null) {
            throw new SubmitEvaluationException("该任务不存在");
        }
        TaskStatus status = TaskStatus.fromCode(task.getStatus());
        if (status != TaskStatus.PENDING) {
            throw new SubmitEvaluationException("该任务已完成或已撤回，不能提交");
        }

        if (task.getCourInfId() == null) {
            throw new SubmitEvaluationException("该任务对应的课程信息不存在，不能提交");
        }
        CourInfDO courInf = courInfMapper.selectById(task.getCourInfId());
        if (courInf == null) {
            throw new SubmitEvaluationException("该任务对应的课程信息不存在，不能提交");
        }
        CourseDO course = courseMapper.selectById(courInf.getCourseId());
        if (course == null) {
            throw new SubmitEvaluationException("该任务对应的课程信息不存在，不能提交");
        }

        FormRecordDO record = new FormRecordDO();
        record.setTaskId(context.taskId());
        record.setTextValue(textValue);
        record.setFormPropsValues(JSONUtil.toJsonStr(formPropsValues));
        formRecordMapper.insert(record);

        task.setStatus(TaskStatus.COMPLETED.code());
        task.setUpdateTime(LocalDateTime.now());
        evaTaskMapper.updateById(task);

        // 缓存失效：尽量保持与历史逻辑一致
        localCacheManager.invalidateCache(evaCacheConstants.ONE_TASK, context.taskId());
        localCacheManager.invalidateCache(null, evaCacheConstants.LOG_LIST);
        String evaluatorName = context.evaluatorName();
        if (evaluatorName == null && task.getTeacherId() != null) {
            SysUserDO user = sysUserMapper.selectById(task.getTeacherId());
            evaluatorName = user == null ? null : user.getName();
        }
        if (evaluatorName != null) {
            localCacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_TEACH, evaluatorName);
        }

        // 首次评教：创建课程模板快照（用于锁定模板）
        CourOneEvaTemplateDO snapshot = courOneEvaTemplateMapper.selectOne(
                new QueryWrapper<CourOneEvaTemplateDO>()
                        .eq("course_id", course.getId())
                        .eq("semester_id", course.getSemesterId())
        );
        if (snapshot == null) {
            FormTemplateDO template = formTemplateMapper.selectById(course.getTemplateId());
            if (template == null) {
                throw new SubmitEvaluationException("评教模板不存在，不能提交");
            }
            CourOneEvaTemplateDO newSnapshot = new CourOneEvaTemplateDO();
            newSnapshot.setCourseId(course.getId());
            newSnapshot.setSemesterId(course.getSemesterId());

            Map<String, Object> templateJson = new LinkedHashMap<>();
            templateJson.put("name", template.getName());
            templateJson.put("description", template.getDescription());
            templateJson.put("props", template.getProps());
            newSnapshot.setFormTemplate(JSONUtil.toJsonStr(templateJson));

            courOneEvaTemplateMapper.insert(newSnapshot);
        }
    }
}

