package edu.cuit.infra.bccourse.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.course.application.port.AssignEvaTeachersRepository;
import edu.cuit.infra.bccourse.support.CourInfTimeOverlapQuery;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.course.SubjectDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.course.SubjectMapper;
import edu.cuit.infra.enums.cache.EvaCacheConstants;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * bc-course：分配听课/评教老师端口适配器（复用现有表结构与规则，行为保持不变）。
 */
@Component
@RequiredArgsConstructor
public class AssignEvaTeachersRepositoryImpl implements AssignEvaTeachersRepository {
    private final CourInfMapper courInfMapper;
    private final CourseMapper courseMapper;
    private final SubjectMapper subjectMapper;
    /**
     * 跨 BC 直连清零（编译期）：不再直接依赖评教侧 Mapper 类型；仍保持原 MyBatis 调用语义，通过反射调用对应方法（保持行为不变）。
     */
    @Autowired
    @Qualifier("evaTaskMapper")
    private Object evaTaskMapper;
    /**
     * SysUserMapper 归位前置（编译期清零）：不再直接依赖 IAM Mapper 类型；仍保持原 MyBatis 调用语义，通过反射调用对应方法（保持行为不变）。
     */
    @Autowired
    @Qualifier("sysUserMapper")
    private Object userMapper;
    private final LocalCacheManager localCacheManager;
    private final EvaCacheConstants evaCacheConstants;

    @Override
    @Transactional
    public Map<String, Map<Integer, Integer>> assign(Integer semesterId, Integer courInfId, List<Integer> evaTeacherIdList) {
        CourInfDO courInfDO = courInfMapper.selectById(courInfId);
        if (courInfDO == null) {
            throw new QueryException("该节课不存在");
        }

        // 查看评教老师在该时间段是否已经有评教任务
        judgeAlsoHasTask(evaTeacherIdList, courInfDO);
        // 判断评教老师在该时间段是否已经有课了
        judgeAlsoHasCourse(semesterId, evaTeacherIdList, courInfDO);

        // 遍历并创建评教任务对象，并插入评教任务表
        List<EvaTaskDO> taskList = evaTeacherIdList.stream().map(id -> {
            EvaTaskDO evaTaskDO = new EvaTaskDO();
            evaTaskDO.setTeacherId(id);
            evaTaskDO.setCourInfId(courInfId);
            evaTaskDO.setStatus(0);
            evaTaskDO.setCreateTime(LocalDateTime.now());
            evaTaskDO.setUpdateTime(LocalDateTime.now());
            return evaTaskDO;
        }).toList();
        taskList.forEach(this::insertEvaTask);

        Map<Integer, Integer> mapTask = new HashMap<>();
        taskList.forEach(evaTaskDO -> mapTask.put(evaTaskDO.getId(), evaTaskDO.getTeacherId()));

        Integer subjectId = courseMapper.selectOne(new QueryWrapper<CourseDO>().eq("id", courInfDO.getCourseId())).getSubjectId();
        String name = subjectMapper.selectOne(new QueryWrapper<SubjectDO>().eq("id", subjectId)).getName();

        Map<String, Map<Integer, Integer>> map = new HashMap<>();
        map.put("你已经被分配去听第" + courInfDO.getWeek() + "周，星期"
                        + courInfDO.getDay() + "，第" + courInfDO.getStartTime() + "-" + courInfDO.getEndTime() + "节，" + name + "课程。位置：" + courInfDO.getLocation(),
                mapTask);

        for (Integer teacherId : evaTeacherIdList) {
            SysUserDO userDO = selectSysUserById(teacherId);
            if (userDO == null) {
                throw new QueryException("所分配老师中有人未在数据库中");
            }
            LogUtils.logContent(userDO.getName() + "老师去听的课：第" + courInfDO.getWeek() + "周，星期"
                    + courInfDO.getDay() + "，第" + courInfDO.getStartTime() + "-" + courInfDO.getEndTime() + "节，" + name + "课程。位置：" + courInfDO.getLocation() + name + "课程");
        }

        localCacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(semesterId));
        return map;
    }

    private SysUserDO selectSysUserById(Serializable userId) {
        try {
            Method selectById = userMapper.getClass().getMethod("selectById", Serializable.class);
            return (SysUserDO) selectById.invoke(userMapper, userId);
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

    private void judgeAlsoHasCourse(Integer semId, List<Integer> evaTeacherIdList, CourInfDO courInfDO) {
        List<Integer> list = courseMapper.selectList(new QueryWrapper<CourseDO>()
                        .eq("semester_id", semId)
                        .in(!evaTeacherIdList.isEmpty(), "teacher_id", evaTeacherIdList))
                .stream()
                .map(CourseDO::getId)
                .toList();
        if (list.isEmpty()) {
            return;
        }
        for (Integer courseId : list) {
            boolean hasCourses = courInfMapper.exists(
                    CourInfTimeOverlapQuery.overlap(
                            courInfDO.getWeek(),
                            courInfDO.getDay(),
                            courInfDO.getStartTime(),
                            courInfDO.getEndTime()
                    ).eq(true, "course_id", courseId)
            );
            if (hasCourses) {
                CourseDO courseDO = courseMapper.selectById(courseId);
                SysUserDO userDO = selectSysUserById(courseDO.getTeacherId());
                throw new UpdateException(userDO.getName() + "老师" + "该时间段已有课程");
            }
        }
    }

    private void judgeAlsoHasTask(List<Integer> userList, CourInfDO courInfDO) {
        List<Integer> courInfoList = selectEvaTaskList(new QueryWrapper<EvaTaskDO>()
                        .in(!userList.isEmpty(), "teacher_id", userList)
                        .eq("status", 0))
                .stream()
                .map(EvaTaskDO::getCourInfId)
                .toList();
        if (courInfoList.isEmpty()) {
            return;
        }
        for (Integer courInfId : courInfoList) {
            CourInfDO courInfDO1 = courInfMapper.selectOne(
                    CourInfTimeOverlapQuery.overlap(
                            courInfDO.getWeek(),
                            courInfDO.getDay(),
                            courInfDO.getStartTime(),
                            courInfDO.getEndTime()
                    ).eq(true, "id", courInfId)
            );
            if (courInfDO1 != null) {
                EvaTaskDO evaTaskDO = selectEvaTaskOne(new QueryWrapper<EvaTaskDO>()
                        .eq("cour_inf_id", courInfDO1.getId())
                        .in("teacher_id", userList));
                SysUserDO userDO = selectSysUserById(evaTaskDO.getTeacherId());
                throw new UpdateException("课程时间冲突，评教老师中" + userDO.getName() + "在该时间段已经有了评教任务");
            }
        }
    }

    private void insertEvaTask(EvaTaskDO evaTaskDO) {
        Method insertMethod = findMethodByNameAndParamCount(evaTaskMapper, "insert", 1);
        if (insertMethod == null) {
            throw new IllegalStateException("evaTaskMapper 缺少 insert(entity) 方法");
        }
        invoke(evaTaskMapper, insertMethod, evaTaskDO);
    }

    private List<EvaTaskDO> selectEvaTaskList(QueryWrapper<EvaTaskDO> qw) {
        Method selectListMethod = findMethodByNameAndParamCount(evaTaskMapper, "selectList", 1);
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

    private EvaTaskDO selectEvaTaskOne(QueryWrapper<EvaTaskDO> qw) {
        Method selectOneMethod = findMethodByNameAndParamCount(evaTaskMapper, "selectOne", 1);
        if (selectOneMethod == null) {
            throw new IllegalStateException("evaTaskMapper 缺少 selectOne(Wrapper) 方法");
        }
        Object result = invoke(evaTaskMapper, selectOneMethod, qw);
        if (result == null) {
            return null;
        }
        return (EvaTaskDO) result;
    }

    private static Method findMethodByNameAndParamCount(Object target, String methodName, int paramCount) {
        if (target == null) {
            return null;
        }
        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == paramCount) {
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
