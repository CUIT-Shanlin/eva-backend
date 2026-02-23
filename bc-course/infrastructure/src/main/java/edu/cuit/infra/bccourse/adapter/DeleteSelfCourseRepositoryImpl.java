package edu.cuit.infra.bccourse.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import edu.cuit.bc.course.application.port.DeleteSelfCourseRepository;
import edu.cuit.bc.iam.application.contract.dto.clientobject.user.UserDetailCO;
import edu.cuit.bc.iam.application.port.UserDetailByUsernameDirectQueryPort;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseTypeCourseDO;
import edu.cuit.infra.dal.database.dataobject.course.SubjectDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormRecordDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseTypeCourseMapper;
import edu.cuit.infra.dal.database.mapper.course.SubjectMapper;
import edu.cuit.infra.enums.cache.ClassroomCacheConstants;
import edu.cuit.infra.enums.cache.CourseCacheConstants;
import edu.cuit.infra.enums.cache.EvaCacheConstants;
import edu.cuit.infra.gateway.impl.course.operate.CourseFormat;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
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
 * bc-course：教师自助删课端口适配器（复用现有表结构与规则，行为保持不变）。
 */
@Component
@RequiredArgsConstructor
public class DeleteSelfCourseRepositoryImpl implements DeleteSelfCourseRepository {
    private final CourInfMapper courInfMapper;
    private final CourseMapper courseMapper;
    private final CourseTypeCourseMapper courseTypeCourseMapper;
    private final SubjectMapper subjectMapper;
    @Autowired
    @Qualifier("evaTaskMapper")
    private Object evaTaskMapper;
    @Autowired
    @Qualifier("formRecordMapper")
    private Object formRecordMapper;
    private final UserDetailByUsernameDirectQueryPort userDetailByUsernameDirectQueryPort;
    private final LocalCacheManager localCacheManager;
    private final CourseCacheConstants courseCacheConstants;
    private final EvaCacheConstants evaCacheConstants;
    private final ClassroomCacheConstants classroomCacheConstants;

    @Override
    @Transactional
    public Map<String, Map<Integer, Integer>> delete(String userName, Integer courseId) {
        if (userName == null) {
            throw new QueryException("请先登录");
        }
        // 先根据 userName 来找到用户id
        UserDetailCO userDetail = userDetailByUsernameDirectQueryPort.findByUsername(userName).orElse(null);
        if (userDetail == null) {
            throw new QueryException("你已经被删除了");
        }
        Integer userId = userDetail.getId().intValue();
        String teacherName = userDetail.getName();
        // 根据 userId 和 courseId 来删除课程表
        CourseDO courseDO = courseMapper.selectOne(new QueryWrapper<CourseDO>().eq("id", courseId).eq("teacher_id", userId));
        if (courseDO == null) {
            throw new QueryException("没有该用户对应课程");
        }
        SubjectDO subjectDO = subjectMapper.selectOne(new QueryWrapper<SubjectDO>().eq("id", courseDO.getSubjectId()));
        String natureName = CourseFormat.getNatureName(subjectDO.getNature());
        String name = subjectDO.getName();

        courseMapper.delete(new UpdateWrapper<CourseDO>().eq("id", courseId).eq("teacher_id", userId));
        if (courseMapper.selectCount(new QueryWrapper<CourseDO>().eq("subject_id", courseDO.getSubjectId())) == 1) {
            subjectMapper.delete(new QueryWrapper<SubjectDO>().eq("id", courseDO.getSubjectId()));
            localCacheManager.invalidateCache(null, courseCacheConstants.SUBJECT_LIST);
        }
        List<CourInfDO> courInfoIds = courInfMapper.selectList(new QueryWrapper<CourInfDO>().eq("course_id", courseId));
        courInfMapper.delete(new UpdateWrapper<CourInfDO>().eq("course_id", courseId));
        courseTypeCourseMapper.delete(new UpdateWrapper<CourseTypeCourseDO>().eq("course_id", courseId));

        // 删除评教相关数据
        List<Integer> list = courInfoIds.stream().map(CourInfDO::getId).toList();
        List<EvaTaskDO> taskDOList = new ArrayList<>();
        if (!list.isEmpty()) {
            taskDOList = (List<EvaTaskDO>) invokeSelectList(
                    evaTaskMapper,
                    new QueryWrapper<EvaTaskDO>().in(true, "cour_inf_id", list)
            );
        }
        if (!taskDOList.isEmpty()) {
            invokeDelete(formRecordMapper, new QueryWrapper<FormRecordDO>().in(true, "task_id", list));
        }
        List<Integer> list1 = courInfoIds.stream().map(CourInfDO::getId).toList();
        if (!list1.isEmpty()) {
            invokeDelete(
                    evaTaskMapper,
                    new UpdateWrapper<EvaTaskDO>().in("cour_inf_id", taskDOList.stream().map(EvaTaskDO::getId).toList())
            );
        }
        Map<Integer, Integer> mapEva = new HashMap<>();
        for (EvaTaskDO i : taskDOList) {
            mapEva.put(i.getId(), i.getTeacherId());
        }
        Map<String, Map<Integer, Integer>> map = new HashMap<>();
        map.put("你所要评教的" + teacherName + "老师的" + name + "课程(" + natureName + ")被删除，已取消评教任务", mapEva);
        map.put(teacherName + "老师的" + name + "课程（" + natureName + "）已被删除", null);

        localCacheManager.invalidateCache(courseCacheConstants.COURSE_LIST_BY_SEM, String.valueOf(courseDO.getSemesterId()));
        localCacheManager.invalidateCache(null, evaCacheConstants.LOG_LIST);
        localCacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(courseDO.getSemesterId()));
        localCacheManager.invalidateCache(null, classroomCacheConstants.ALL_CLASSROOM);
        return map;
    }

    private static Object invokeSelectList(Object mapper, Object queryWrapper) {
        try {
            Method method = resolveSingleArgMethod(mapper, "selectList", queryWrapper);
            return method.invoke(mapper, queryWrapper);
        } catch (InvocationTargetException e) {
            sneakyThrow(e.getTargetException());
            return null;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Object invokeDelete(Object mapper, Object queryWrapper) {
        try {
            Method method = resolveSingleArgMethod(mapper, "delete", queryWrapper);
            return method.invoke(mapper, queryWrapper);
        } catch (InvocationTargetException e) {
            sneakyThrow(e.getTargetException());
            return null;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Method resolveSingleArgMethod(Object target, String methodName, Object arg) throws NoSuchMethodException {
        Class<?> argClass = arg.getClass();
        for (Method method : target.getClass().getMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            if (method.getParameterCount() != 1) {
                continue;
            }
            Class<?> paramType = method.getParameterTypes()[0];
            if (paramType.isAssignableFrom(argClass)) {
                return method;
            }
        }
        throw new NoSuchMethodException(target.getClass().getName() + "#" + methodName + "(" + argClass.getName() + ")");
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable throwable) throws T {
        throw (T) throwable;
    }
}
