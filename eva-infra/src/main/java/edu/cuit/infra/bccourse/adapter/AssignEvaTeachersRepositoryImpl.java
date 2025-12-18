package edu.cuit.infra.bccourse.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.course.application.port.AssignEvaTeachersRepository;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.course.SubjectDO;
import edu.cuit.infra.dal.database.dataobject.eva.EvaTaskDO;
import edu.cuit.infra.dal.database.dataobject.user.SysUserDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.course.SubjectMapper;
import edu.cuit.infra.dal.database.mapper.eva.EvaTaskMapper;
import edu.cuit.infra.dal.database.mapper.user.SysUserMapper;
import edu.cuit.infra.enums.cache.EvaCacheConstants;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private final EvaTaskMapper evaTaskMapper;
    private final SysUserMapper userMapper;
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
        taskList.forEach(evaTaskMapper::insert);

        Map<Integer, Integer> mapTask = new HashMap<>();
        taskList.forEach(evaTaskDO -> mapTask.put(evaTaskDO.getId(), evaTaskDO.getTeacherId()));

        Integer subjectId = courseMapper.selectOne(new QueryWrapper<CourseDO>().eq("id", courInfDO.getCourseId())).getSubjectId();
        String name = subjectMapper.selectOne(new QueryWrapper<SubjectDO>().eq("id", subjectId)).getName();

        Map<String, Map<Integer, Integer>> map = new HashMap<>();
        map.put("你已经被分配去听第" + courInfDO.getWeek() + "周，星期"
                        + courInfDO.getDay() + "，第" + courInfDO.getStartTime() + "-" + courInfDO.getEndTime() + "节，" + name + "课程。位置：" + courInfDO.getLocation(),
                mapTask);

        for (Integer teacherId : evaTeacherIdList) {
            SysUserDO userDO = userMapper.selectById(teacherId);
            if (userDO == null) {
                throw new QueryException("所分配老师中有人未在数据库中");
            }
            LogUtils.logContent(userDO.getName() + "老师去听的课：第" + courInfDO.getWeek() + "周，星期"
                    + courInfDO.getDay() + "，第" + courInfDO.getStartTime() + "-" + courInfDO.getEndTime() + "节，" + name + "课程。位置：" + courInfDO.getLocation() + name + "课程");
        }

        localCacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(semesterId));
        return map;
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
            boolean hasCourses = courInfMapper.exists(new QueryWrapper<CourInfDO>()
                    .eq("week", courInfDO.getWeek())
                    .eq("day", courInfDO.getDay())
                    .le("start_time", courInfDO.getEndTime())
                    .ge("end_time", courInfDO.getStartTime())
                    .eq(true, "course_id", courseId));
            if (hasCourses) {
                CourseDO courseDO = courseMapper.selectById(courseId);
                SysUserDO userDO = userMapper.selectById(courseDO.getTeacherId());
                throw new UpdateException(userDO.getName() + "老师" + "该时间段已有课程");
            }
        }
    }

    private void judgeAlsoHasTask(List<Integer> userList, CourInfDO courInfDO) {
        List<Integer> courInfoList = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>()
                        .in(!userList.isEmpty(), "teacher_id", userList)
                        .eq("status", 0))
                .stream()
                .map(EvaTaskDO::getCourInfId)
                .toList();
        if (courInfoList.isEmpty()) {
            return;
        }
        for (Integer courInfId : courInfoList) {
            CourInfDO courInfDO1 = courInfMapper.selectOne(new QueryWrapper<CourInfDO>()
                    .eq("week", courInfDO.getWeek())
                    .eq("day", courInfDO.getDay())
                    .le("start_time", courInfDO.getEndTime())
                    .ge("end_time", courInfDO.getStartTime())
                    .eq(true, "id", courInfId));
            if (courInfDO1 != null) {
                EvaTaskDO evaTaskDO = evaTaskMapper.selectOne(new QueryWrapper<EvaTaskDO>()
                        .eq("cour_inf_id", courInfDO1.getId())
                        .in("teacher_id", userList));
                SysUserDO userDO = userMapper.selectById(evaTaskDO.getTeacherId());
                throw new UpdateException("课程时间冲突，评教老师中" + userDO.getName() + "在该时间段已经有了评教任务");
            }
        }
    }
}

