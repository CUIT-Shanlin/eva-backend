package edu.cuit.infra.bcevaluation.repository;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.course.application.port.CourseIdByCourInfIdQueryPort;
import edu.cuit.bc.course.application.port.CourseTeacherAndSemesterQueryPort;
import edu.cuit.bc.course.application.port.CourseTemplateIdQueryPort;
import edu.cuit.bc.evaluation.application.model.FormPropValue;
import edu.cuit.bc.evaluation.application.model.SubmitEvaluationContext;
import edu.cuit.bc.evaluation.application.port.SubmitEvaluationRepository;
import edu.cuit.bc.evaluation.domain.SubmitEvaluationException;
import edu.cuit.bc.evaluation.domain.TaskStatus;
import edu.cuit.infra.dal.database.dataobject.eva.CourOneEvaTemplateDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormRecordDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormTemplateDO;
import edu.cuit.infra.dal.database.mapper.eva.CourOneEvaTemplateMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormRecordMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormTemplateMapper;
import edu.cuit.infra.enums.cache.EvaCacheConstants;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
    private final CourseIdByCourInfIdQueryPort courseIdByCourInfIdQueryPort;
    private final CourseTeacherAndSemesterQueryPort courseTeacherAndSemesterQueryPort;
    private final CourseTemplateIdQueryPort courseTemplateIdQueryPort;
    private final FormTemplateMapper formTemplateMapper;
    private final CourOneEvaTemplateMapper courOneEvaTemplateMapper;
    @Autowired
    @Qualifier("sysUserMapper")
    private Object sysUserMapper;
    private final EvaCacheConstants evaCacheConstants;
    private final LocalCacheManager localCacheManager;

    @Override
    public SubmitEvaluationContext loadContext(Integer taskId) {
        EvaTaskDO task = evaTaskMapper.selectById(taskId);
        if (task == null) {
            return null;
        }

        Object evaluator = selectSysUserById(task.getTeacherId());
        Integer courseId = task.getCourInfId() == null
                ? null
                : courseIdByCourInfIdQueryPort.findCourseIdByCourInfId(task.getCourInfId()).orElse(null);
        CourseTeacherAndSemesterQueryPort.CourseTeacherAndSemester courseTeacherAndSemester = courseId == null
                ? null
                : courseTeacherAndSemesterQueryPort.findByCourseId(courseId).orElse(null);
        Integer semesterId = courseTeacherAndSemester == null ? null : courseTeacherAndSemester.semesterId();
        Integer templateId = courseTeacherAndSemester == null
                ? null
                : courseTemplateIdQueryPort.findTemplateId(semesterId, courseId).orElse(null);
        FormTemplateDO template = templateId == null ? null : formTemplateMapper.selectById(templateId);

        return new SubmitEvaluationContext(
                taskId,
                task.getTeacherId(),
                evaluator == null ? null : selectSysUserName(evaluator),
                task.getCourInfId(),
                courseId,
                semesterId,
                templateId,
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
        Integer courseId = courseIdByCourInfIdQueryPort.findCourseIdByCourInfId(task.getCourInfId()).orElse(null);
        if (courseId == null) {
            throw new SubmitEvaluationException("该任务对应的课程信息不存在，不能提交");
        }
        CourseTeacherAndSemesterQueryPort.CourseTeacherAndSemester courseTeacherAndSemester = courseTeacherAndSemesterQueryPort
                .findByCourseId(courseId)
                .orElse(null);
        if (courseTeacherAndSemester == null) {
            throw new SubmitEvaluationException("该任务对应的课程信息不存在，不能提交");
        }
        Integer semesterId = courseTeacherAndSemester.semesterId();

        FormRecordDO record = new FormRecordDO();
        record.setTaskId(context.taskId());
        record.setTextValue(textValue);
        record.setFormPropsValues(JSONUtil.toJsonStr(formPropsValues));
        formRecordMapper.insert(record);

        task.setStatus(TaskStatus.COMPLETED.code());
        task.setUpdateTime(LocalDateTime.now());
        evaTaskMapper.updateById(task);

        // 缓存失效：尽量保持与历史逻辑一致
        localCacheManager.invalidateCache(evaCacheConstants.ONE_TASK, String.valueOf(context.taskId()));
        localCacheManager.invalidateCache(null, evaCacheConstants.LOG_LIST);
        String evaluatorName = context.evaluatorName();
        if (evaluatorName == null && task.getTeacherId() != null) {
            Object user = selectSysUserById(task.getTeacherId());
            evaluatorName = user == null ? null : selectSysUserName(user);
        }
        if (evaluatorName != null) {
            localCacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_TEACH, evaluatorName);
        }

        // 首次评教：创建课程模板快照（用于锁定模板）
        CourOneEvaTemplateDO snapshot = courOneEvaTemplateMapper.selectOne(
                new QueryWrapper<CourOneEvaTemplateDO>()
                        .eq("course_id", courseId)
                        .eq("semester_id", semesterId)
        );
        if (snapshot == null) {
            Integer templateId = courseTemplateIdQueryPort.findTemplateId(semesterId, courseId).orElse(null);
            FormTemplateDO template = formTemplateMapper.selectById(templateId);
            if (template == null) {
                throw new SubmitEvaluationException("评教模板不存在，不能提交");
            }
            CourOneEvaTemplateDO newSnapshot = new CourOneEvaTemplateDO();
            newSnapshot.setCourseId(courseId);
            newSnapshot.setSemesterId(semesterId);

            Map<String, Object> templateJson = new LinkedHashMap<>();
            templateJson.put("name", template.getName());
            templateJson.put("description", template.getDescription());
            templateJson.put("props", template.getProps());
            newSnapshot.setFormTemplate(JSONUtil.toJsonStr(templateJson));

            courOneEvaTemplateMapper.insert(newSnapshot);
        }
    }

    private Object selectSysUserById(Serializable userId) {
        try {
            Method selectById = sysUserMapper.getClass().getMethod("selectById", Serializable.class);
            return selectById.invoke(sysUserMapper, userId);
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (targetException instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (targetException instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(targetException);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private String selectSysUserName(Object sysUser) {
        try {
            Method getName = sysUser.getClass().getMethod("getName");
            return (String) getName.invoke(sysUser);
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (targetException instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (targetException instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(targetException);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
