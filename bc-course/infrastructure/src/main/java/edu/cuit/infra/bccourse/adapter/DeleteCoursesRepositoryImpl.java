package edu.cuit.infra.bccourse.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.course.application.port.DeleteCoursesRepository;
import edu.cuit.bc.iam.application.port.UserNameDirectQueryPort;
import edu.cuit.client.dto.data.course.CoursePeriod;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.course.SubjectDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormRecordDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.course.SubjectMapper;
import edu.cuit.infra.enums.cache.ClassroomCacheConstants;
import edu.cuit.infra.enums.cache.EvaCacheConstants;
import edu.cuit.infra.gateway.impl.course.operate.CourseFormat;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * bc-course：批量删除某节课端口适配器（复用现有表结构与规则，行为保持不变）。
 */
@Component
@RequiredArgsConstructor
public class DeleteCoursesRepositoryImpl implements DeleteCoursesRepository {
    private final CourInfMapper courInfMapper;
    private final CourseMapper courseMapper;
    private final SubjectMapper subjectMapper;
    private final UserNameDirectQueryPort userNameDirectQueryPort;
    /**
     * 跨 BC 直连清零（编译期）：不再直接依赖评教侧 Mapper 类型；仍保持原 MyBatis 调用语义，通过反射调用对应方法（保持行为不变）。
     */
    @Autowired
    @Qualifier("evaTaskMapper")
    private Object evaTaskMapper;
    @Autowired
    @Qualifier("formRecordMapper")
    private Object formRecordMapper;
    private final LocalCacheManager localCacheManager;
    private final EvaCacheConstants evaCacheConstants;
    private final ClassroomCacheConstants classroomCacheConstants;

    @Override
    @Transactional
    public Map<String, Map<Integer, Integer>> delete(Integer semId, Integer id, CoursePeriod coursePeriod) {
        CourInfDO courInfDO = courInfMapper.selectById(id);
        if (courInfDO == null) {
            throw new UpdateException("该节课不存在");
        }
        id = courInfDO.getCourseId();
        CourseDO courseDO = courseMapper.selectOne(new QueryWrapper<CourseDO>().eq("id", id).eq("semester_id", semId));
        if (courseDO == null) {
            throw new QueryException("课程不存在");
        }
        String teacherName;
        try {
            teacherName = userNameDirectQueryPort.findNameById(courseDO.getTeacherId());
        } catch (NullPointerException e) {
            throw new QueryException("对应老师不存在");
        }
        SubjectDO subjectDO = subjectMapper.selectOne(new QueryWrapper<SubjectDO>().eq("id", courseDO.getSubjectId()));
        String natureName = CourseFormat.getNatureName(subjectDO.getNature());
        String name = subjectDO.getName();

        // id来找出课程数据
        QueryWrapper<CourInfDO> courseWrapper = new QueryWrapper<>();
        courseWrapper.eq("course_id", id);
        isEmptiy(courseWrapper, coursePeriod);
        List<Integer> list = courInfMapper.selectList(courseWrapper).stream().map(CourInfDO::getId).toList();
        courInfMapper.delete(courseWrapper);

        // 找出所有要评教这节课的老师
        List<EvaTaskDO> tasks;
        if (!list.isEmpty()) {
            tasks = selectEvaTaskList(new QueryWrapper<EvaTaskDO>().in("cour_inf_id", list));
            deleteEvaTasksByCourInfIds(list);
        } else {
            tasks = new ArrayList<>();
        }

        // 删除评教记录
        if (!tasks.isEmpty()) {
            deleteFormRecordsByTaskIds(tasks.stream().map(EvaTaskDO::getId).toList());
        }

        Map<Integer, Integer> mapEva = new HashMap<>();
        for (EvaTaskDO task : tasks) {
            mapEva.put(task.getId(), task.getTeacherId());
        }
        Map<String, Map<Integer, Integer>> map = new HashMap<>();
        map.put(teacherName + "老师的" + name + "课程(" + natureName + ")的一些课程已被删除", null);
        map.put("你所评教的" + teacherName + "老师的" + "上课时间在第" + coursePeriod.getStartWeek() + "周，星期" + coursePeriod.getDay()
                + "，第" + coursePeriod.getStartTime() + "-" + coursePeriod.getEndTime() + "节，" + name + "课程已经被删除，故已取消您对该课程的评教任务", mapEva);
        LogUtils.logContent(teacherName + "老师-" + name + "(课程ID:" + id + ")的一些课");
        localCacheManager.invalidateCache(null, evaCacheConstants.LOG_LIST);
        localCacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(semId));
        localCacheManager.invalidateCache(null, classroomCacheConstants.ALL_CLASSROOM);
        return map;
    }

    private void isEmptiy(QueryWrapper wrapper, CoursePeriod coursePeriod) {
        if (coursePeriod.getStartWeek() != null) {
            wrapper.ge("week", coursePeriod.getStartWeek());
        }
        if (coursePeriod.getEndWeek() != null) {
            wrapper.le("week", coursePeriod.getEndWeek());
        }
        if (coursePeriod.getDay() != null) {
            wrapper.eq("day", coursePeriod.getDay());
        }
        if (coursePeriod.getStartTime() != null) {
            wrapper.eq("start_time", coursePeriod.getStartTime());
        }
        if (coursePeriod.getEndTime() != null) {
            wrapper.eq("end_time", coursePeriod.getEndTime());
        }
    }

    private List<EvaTaskDO> selectEvaTaskList(QueryWrapper<EvaTaskDO> qw) {
        Method selectListMethod = findSingleArgMethod(evaTaskMapper, "selectList");
        if (selectListMethod == null) {
            throw new IllegalStateException("evaTaskMapper 缺少 selectList(Wrapper) 方法");
        }
        Object result = invoke(evaTaskMapper, selectListMethod, qw);
        if (!(result instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(EvaTaskDO.class::isInstance)
                .map(EvaTaskDO.class::cast)
                .toList();
    }

    private void deleteEvaTasksByCourInfIds(List<Integer> courInfIds) {
        Method deleteMethod = findSingleArgMethod(evaTaskMapper, "delete");
        if (deleteMethod == null) {
            throw new IllegalStateException("evaTaskMapper 缺少 delete(Wrapper) 方法");
        }
        invoke(evaTaskMapper, deleteMethod, new QueryWrapper<EvaTaskDO>().in("cour_inf_id", courInfIds));
    }

    private void deleteFormRecordsByTaskIds(List<Integer> taskIds) {
        Method deleteMethod = findSingleArgMethod(formRecordMapper, "delete");
        if (deleteMethod == null) {
            throw new IllegalStateException("formRecordMapper 缺少 delete(Wrapper) 方法");
        }
        invoke(formRecordMapper, deleteMethod, new QueryWrapper<FormRecordDO>().in("task_id", taskIds));
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
