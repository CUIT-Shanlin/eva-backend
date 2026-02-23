package edu.cuit.infra.bccourse.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import edu.cuit.bc.course.application.port.DeleteCourseRepository;
import edu.cuit.bc.evaluation.application.port.EvaTaskCascadeDeleteByTaskIdsPort;
import edu.cuit.bc.evaluation.application.port.EvaTaskBriefByCourInfIdsDirectQueryPort;
import edu.cuit.bc.iam.application.port.UserNameDirectQueryPort;
import edu.cuit.client.dto.clientobject.eva.EvaTaskBriefCO;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.course.SubjectDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.course.SubjectMapper;
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
    private final UserNameDirectQueryPort userNameDirectQueryPort;
    private final EvaTaskBriefByCourInfIdsDirectQueryPort evaTaskBriefByCourInfIdsDirectQueryPort;
    private final EvaTaskCascadeDeleteByTaskIdsPort evaTaskCascadeDeleteByTaskIdsPort;
    private final LocalCacheManager localCacheManager;
    private final CourseCacheConstants courseCacheConstants;
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
        String teacherName = null;
        boolean teacherNotFound = false;
        try {
            teacherName = userNameDirectQueryPort.findNameById(courseDO.getTeacherId());
        } catch (NullPointerException e) {
            // 保持行为不变：原逻辑 teacherId 未命中时，异常会在 userDO.getName() 处触发（发生在删除动作之后）。
            // 这里先吞掉 NPE 并继续执行删除链路，最后在首次使用 teacherName 前再按同口径抛出。
            teacherNotFound = true;
        }
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
        List<EvaTaskBriefCO> taskBriefList;
        if (!list.isEmpty()) {
            taskBriefList = evaTaskBriefByCourInfIdsDirectQueryPort.findTaskBriefListByCourInfIds(list);
        } else {
            taskBriefList = List.of();
        }

        List<Integer> taskIds = taskBriefList.stream().map(EvaTaskBriefCO::getId).toList();
        List<EvaTaskBriefCO> list1 = taskBriefList.stream().filter(taskDO -> taskDO.getStatus() == 0).toList();
        if (!taskIds.isEmpty()) {
            evaTaskCascadeDeleteByTaskIdsPort.deleteCascadeByTaskIds(taskIds);
        }

        Map<String, Map<Integer, Integer>> map = new HashMap<>();
        Map<Integer, Integer> evaTaskMap = new HashMap<>();
        for (EvaTaskBriefCO taskDO : list1) {
            evaTaskMap.put(taskDO.getId(), taskDO.getTeacherId());
        }

        if (teacherNotFound) {
            throw new NullPointerException();
        }
        map.put(teacherName + "老师的" + name + "课程（" + natureName + "）被删除", null);
        map.put("因为" + teacherName + "老师的" + name + "课程已被删除，" + "故已取消您对该课程的评教任务,和评教记录", evaTaskMap);
        LogUtils.logContent(name + "(课程ID:" + id + ")这门课");
        localCacheManager.invalidateCache(null, evaCacheConstants.LOG_LIST);
        localCacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(semId));
        localCacheManager.invalidateCache(null, classroomCacheConstants.ALL_CLASSROOM);
        return map;
    }
}
