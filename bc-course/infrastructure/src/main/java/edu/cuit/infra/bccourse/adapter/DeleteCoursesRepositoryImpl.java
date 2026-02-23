package edu.cuit.infra.bccourse.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.course.application.port.DeleteCoursesRepository;
import edu.cuit.bc.evaluation.application.port.EvaTaskBriefByCourInfIdsDirectQueryPort;
import edu.cuit.bc.evaluation.application.port.EvaTaskCascadeDeleteByTaskIdsPort;
import edu.cuit.bc.iam.application.port.UserNameDirectQueryPort;
import edu.cuit.client.dto.clientobject.eva.EvaTaskBriefCO;
import edu.cuit.client.dto.data.course.CoursePeriod;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.course.SubjectDO;
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private final EvaTaskBriefByCourInfIdsDirectQueryPort evaTaskBriefByCourInfIdsDirectQueryPort;
    private final EvaTaskCascadeDeleteByTaskIdsPort evaTaskCascadeDeleteByTaskIdsPort;
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
        List<EvaTaskBriefCO> tasks;
        if (!list.isEmpty()) {
            tasks = evaTaskBriefByCourInfIdsDirectQueryPort.findTaskBriefListByCourInfIds(list);
        } else {
            tasks = new ArrayList<>();
        }

        // 删除评教任务与评教记录
        if (!tasks.isEmpty()) {
            evaTaskCascadeDeleteByTaskIdsPort.deleteCascadeByTaskIds(tasks.stream().map(EvaTaskBriefCO::getId).toList());
        }

        Map<Integer, Integer> mapEva = new HashMap<>();
        for (EvaTaskBriefCO task : tasks) {
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
}
