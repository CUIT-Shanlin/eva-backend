package edu.cuit.infra.bccourse.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import edu.cuit.bc.course.application.port.DeleteCourseRepository;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.course.SubjectDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.eva.FormRecordDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.course.SubjectMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.eva.FormRecordMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.infra.enums.cache.ClassroomCacheConstants;
import edu.cuit.infra.enums.cache.CourseCacheConstants;
import edu.cuit.infra.enums.cache.EvaCacheConstants;
import edu.cuit.infra.gateway.impl.course.operate.CourseFormat;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * bc-course：连带删除一门课程端口适配器（复用现有表结构与规则，行为保持不变）。
 */
@Component
@RequiredArgsConstructor
public class DeleteCourseRepositoryImpl implements DeleteCourseRepository {
    private final CourInfMapper courInfMapper;
    private final CourseMapper courseMapper;
    private final SubjectMapper subjectMapper;
    private final EvaTaskMapper evaTaskMapper;
    private final SysUserMapper userMapper;
    private final LocalCacheManager localCacheManager;
    private final CourseCacheConstants courseCacheConstants;
    private final FormRecordMapper formRecordMapper;
    private final EvaCacheConstants evaCacheConstants;
    private final ClassroomCacheConstants classroomCacheConstants;

    @Override
    @Transactional
    public Map<String, Map<Integer, Integer>> delete(Integer semId, Integer id) {
        // 删除课程表
        QueryWrapper<CourseDO> courseWrapper = new QueryWrapper<>();
        courseWrapper.eq("id", id);
        if (semId != null) {
            courseWrapper.eq("semester_id", semId);
        }
        CourseDO courseDO = courseMapper.selectOne(courseWrapper);
        if (courseDO == null) {
            throw new QueryException("课程已经被删除，或者不存在");
        }
        SysUserDO userDO = userMapper.selectById(courseDO.getTeacherId());
        SubjectDO subjectDO = subjectMapper.selectOne(new QueryWrapper<SubjectDO>().eq("id", courseDO.getSubjectId()));
        String natureName = CourseFormat.getNatureName(subjectDO.getNature());
        String name = subjectDO.getName();

        int delete = courseMapper.delete(courseWrapper);
        if (delete == 0) {
            throw new UpdateException("该课程不存在");
        }
        if (!courseMapper.exists(new QueryWrapper<CourseDO>().eq("subject_id", courseDO.getSubjectId()))) {
            subjectMapper.delete(new QueryWrapper<SubjectDO>().eq("id", courseDO.getSubjectId()));
            localCacheManager.invalidateCache(null, courseCacheConstants.SUBJECT_LIST);
        }

        localCacheManager.invalidateCache(courseCacheConstants.COURSE_LIST_BY_SEM, String.valueOf(semId));
        // 删除课程详情表
        UpdateWrapper<CourInfDO> courInfoWrapper = new UpdateWrapper<>();
        courInfoWrapper.eq("course_id", id);
        List<Integer> list = courInfMapper.selectList(courInfoWrapper).stream().map(CourInfDO::getId).toList();
        courInfMapper.delete(courInfoWrapper);

        // 删除评教任务数据
        List<EvaTaskDO> taskDOList;
        if (!list.isEmpty()) {
            QueryWrapper<EvaTaskDO> evaTaskWrapper = new QueryWrapper<>();
            evaTaskWrapper.in("cour_inf_id", list);
            taskDOList = evaTaskMapper.selectList(evaTaskWrapper);
        } else {
            taskDOList = new ArrayList<>();
        }

        List<Integer> taskIds = taskDOList.stream().map(EvaTaskDO::getId).toList();
        List<EvaTaskDO> list1 = taskDOList.stream().filter(taskDO -> taskDO.getStatus() == 0).toList();
        if (!taskIds.isEmpty()) {
            evaTaskMapper.delete(new QueryWrapper<EvaTaskDO>().in("id", taskIds));
            formRecordMapper.delete(new QueryWrapper<FormRecordDO>().in("task_id", taskIds));
        }

        Map<String, Map<Integer, Integer>> map = new HashMap<>();
        Map<Integer, Integer> evaTaskMap = new HashMap<>();
        for (EvaTaskDO taskDO : list1) {
            evaTaskMap.put(taskDO.getId(), taskDO.getTeacherId());
        }

        map.put(userDO.getName() + "老师的" + name + "课程（" + natureName + "）被删除", null);
        map.put("因为" + userDO.getName() + "老师的" + name + "课程已被删除，" + "故已取消您对该课程的评教任务,和评教记录", evaTaskMap);
        LogUtils.logContent(name + "(课程ID:" + id + ")这门课");
        localCacheManager.invalidateCache(null, evaCacheConstants.LOG_LIST);
        localCacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(semId));
        localCacheManager.invalidateCache(null, classroomCacheConstants.ALL_CLASSROOM);
        return map;
    }
}

