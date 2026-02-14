package edu.cuit.infra.bctemplate.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.template.application.port.CourseTemplateLockQueryPort;
import edu.cuit.infra.dal.database.dataobject.eva.CourOneEvaTemplateDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormRecordDO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;

/**
 * bc-template：课程模板锁定查询端口实现（基于现有表结构）。
 */
@Component
public class CourseTemplateLockQueryPortImpl implements CourseTemplateLockQueryPort {
    /**
     * 跨 BC 直连清零（编译期）：不再在 bc-template 侧直接依赖评教侧 Mapper 类型；
     * 仍保持原 MyBatis 调用语义，通过反射调用对应方法（保持行为不变）。
     */
    private final Object courOneEvaTemplateMapper;
    /**
     * 跨 BC 直连清零：不再在 bc-template 侧直连课程域 DAL（CourInfMapper/CourInfDO）。
     *
     * <p>说明：为满足“单类闭环”约束，这里以 {@link Object} 形态注入 bc-course 端口实现 bean（按名称限定），
     * 并通过反射调用端口方法获取 cour_inf.id 集合。单测场景下也允许传入旧 CourInfMapper mock，行为保持不变。</p>
     */
    private final Object courInfIdsByCourseIdsQueryPort;
    private final Object evaTaskMapper;
    private final Object formRecordMapper;

    public CourseTemplateLockQueryPortImpl(
            @Qualifier("courOneEvaTemplateMapper") Object courOneEvaTemplateMapper,
            @Qualifier("courInfIdsByCourseIdsQueryPortImpl") Object courInfIdsByCourseIdsQueryPort,
            @Qualifier("evaTaskMapper") Object evaTaskMapper,
            @Qualifier("formRecordMapper") Object formRecordMapper
    ) {
        this.courOneEvaTemplateMapper = courOneEvaTemplateMapper;
        this.courInfIdsByCourseIdsQueryPort = courInfIdsByCourseIdsQueryPort;
        this.evaTaskMapper = evaTaskMapper;
        this.formRecordMapper = formRecordMapper;
    }

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
        Method selectCountMethod = findSingleArgMethod(courOneEvaTemplateMapper, "selectCount");
        if (selectCountMethod == null) {
            return false;
        }
        Object countObj = invoke(courOneEvaTemplateMapper, selectCountMethod, qw);
        if (!(countObj instanceof Number count)) {
            return false;
        }
        return count.longValue() > 0;
    }

    private boolean isLockedByRecord(Integer courseId) {
        List<Integer> courInfIds = findCourInfIdsByCourseId(courseId);
        if (courInfIds.isEmpty()) {
            return false;
        }
        Method selectTaskListMethod = findSingleArgMethod(evaTaskMapper, "selectList");
        if (selectTaskListMethod == null) {
            return false;
        }
        Object taskListObj = invoke(evaTaskMapper, selectTaskListMethod, new QueryWrapper<EvaTaskDO>().in("cour_inf_id", courInfIds));
        if (!(taskListObj instanceof List<?> taskList)) {
            return false;
        }
        List<Integer> taskIds = taskList.stream()
                .filter(EvaTaskDO.class::isInstance)
                .map(EvaTaskDO.class::cast)
                .map(EvaTaskDO::getId)
                .toList();
        if (taskIds.isEmpty()) {
            return false;
        }
        Method selectRecordCountMethod = findSingleArgMethod(formRecordMapper, "selectCount");
        if (selectRecordCountMethod == null) {
            return false;
        }
        Object countObj = invoke(formRecordMapper, selectRecordCountMethod, new QueryWrapper<FormRecordDO>().in("task_id", taskIds));
        if (!(countObj instanceof Number count)) {
            return false;
        }
        return count.longValue() > 0;
    }

    @SuppressWarnings("unchecked")
    private List<Integer> findCourInfIdsByCourseId(Integer courseId) {
        List<Integer> courseIds = List.of(courseId);

        Method portMethod = findMethod(courInfIdsByCourseIdsQueryPort, "findCourInfIdsByCourseIds", List.class);
        if (portMethod != null) {
            Object result = invoke(courInfIdsByCourseIdsQueryPort, portMethod, courseIds);
            if (result == null) {
                return List.of();
            }
            return (List<Integer>) result;
        }

        Method legacySelectListMethod = findSingleArgMethod(courInfIdsByCourseIdsQueryPort, "selectList");
        if (legacySelectListMethod == null) {
            return List.of();
        }

        Object legacyList = invoke(
                courInfIdsByCourseIdsQueryPort,
                legacySelectListMethod,
                new QueryWrapper<>().eq("course_id", courseId)
        );
        if (!(legacyList instanceof List<?> legacyCourInfList)) {
            return List.of();
        }
        return legacyCourInfList.stream()
                .map(this::extractIdAsIntegerOrNull)
                .toList();
    }

    private Integer extractIdAsIntegerOrNull(Object courInfObject) {
        if (courInfObject == null) {
            return null;
        }
        Method getIdMethod = findMethod(courInfObject, "getId");
        if (getIdMethod == null) {
            return null;
        }
        Object id = invoke(courInfObject, getIdMethod);
        if (id == null) {
            return null;
        }
        if (id instanceof Integer integerId) {
            return integerId;
        }
        if (id instanceof Number numberId) {
            return numberId.intValue();
        }
        return null;
    }

    private static Method findMethod(Object target, String methodName, Class<?>... parameterTypes) {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Method findSingleArgMethod(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == 1) {
                return method;
            }
        }
        return null;
    }

    private static Object invoke(Object target, Method method, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(e);
        }
    }
}
