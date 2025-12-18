package edu.cuit.infra.bccourse.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.course.application.model.UpdateSingleCourseCommand;
import edu.cuit.bc.course.application.port.UpdateSingleCourseRepository;
import edu.cuit.infra.bccourse.support.CourInfTimeOverlapQuery;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * bc-course：改课端口适配器（复用现有表结构与规则，行为保持不变）。
 */
@Component
@RequiredArgsConstructor
public class UpdateSingleCourseRepositoryImpl implements UpdateSingleCourseRepository {
    private final CourInfMapper courInfMapper;
    private final CourseMapper courseMapper;
    private final SubjectMapper subjectMapper;
    private final SysUserMapper userMapper;
    private final EvaTaskMapper evaTaskMapper;
    private final LocalCacheManager localCacheManager;
    private final EvaCacheConstants evaCacheConstants;
    private final ClassroomCacheConstants classroomCacheConstants;

    @Override
    @Transactional
    public Map<String, Map<Integer, Integer>> update(UpdateSingleCourseCommand command) {
        Integer semId = command.semesterId();
        Integer courInfId = command.courInfId();

        // 先将要修改的那节课查出来
        CourInfDO courINfo = courInfMapper.selectById(courInfId);
        CourseDO courseDo = null;
        if (courINfo == null) {
            throw new QueryException("该节课不存在");
        }

        // 先根据课次所属课程找到教师信息（原逻辑如此，保持不变）
        CourseDO courseOfCourInf = courseMapper.selectById(courINfo.getCourseId());
        SysUserDO userDO = userMapper.selectById(courseOfCourInf.getTeacherId());
        if (userDO == null) {
            throw new QueryException("老师不存在");
        }
        Integer teacherId = userDO.getId();

        // 根据 teacherId 和 semId 找出他的所有授课
        List<CourseDO> courseDOList = courseMapper.selectList(new QueryWrapper<CourseDO>()
                .eq("teacher_id", teacherId)
                .eq("semester_id", semId));

        // 判断该老师在新时间段是否有其它课程（排除当前课程）
        for (CourseDO courseDO : courseDOList) {
            if (Objects.equals(courseDO.getId(), courINfo.getCourseId())) {
                courseDo = courseDO;
                continue;
            }
            CourInfDO courInfDO = courInfMapper.selectOne(
                    CourInfTimeOverlapQuery.overlap(command.week(), command.day(), command.startTime(), command.endTime())
                            .eq("course_id", courseDO.getId())
            );
            if (courInfDO != null) {
                throw new UpdateException("该时间段已有课程");
            }
        }

        // 判断 location 是否被占用（原逻辑：同周同天同节次 + 同学期 + 同地点）
        List<CourInfDO> courInfDOList = courInfMapper.selectList(
                CourInfTimeOverlapQuery.overlap(command.week(), command.day(), command.startTime(), command.endTime())
        );
        for (CourInfDO courInfDO : courInfDOList) {
            if (courseMapper.selectById(courInfDO.getCourseId()).getSemesterId().equals(semId)) {
                if (courInfDO.getLocation().equals(command.location())) {
                    throw new UpdateException("该时间段该地点已有课程");
                }
            }
        }

        if (courseDo == null) {
            throw new QueryException("没有该课程");
        }
        SubjectDO subjectDO = subjectMapper.selectOne(new QueryWrapper<SubjectDO>().eq("id", courseDo.getSubjectId()));
        String natureName = CourseFormat.getNatureName(subjectDO.getNature());
        String name = subjectDO.getName();

        // 更新一节课的数据
        CourInfDO update = new CourInfDO();
        update.setWeek(command.week());
        update.setDay(command.day());
        update.setStartTime(command.startTime());
        update.setEndTime(command.endTime());
        update.setUpdateTime(LocalDateTime.now());
        update.setLocation(command.location());
        courInfMapper.update(update, new QueryWrapper<CourInfDO>().eq("id", courInfId));

        // 找出所有要评教这节课的老师id（注意：原逻辑使用 cour_inf_id = courseId，这里保持不变）
        List<EvaTaskDO> taskDOList = evaTaskMapper.selectList(new QueryWrapper<EvaTaskDO>()
                .eq("cour_inf_id", courINfo.getCourseId())
                .eq("status", 0));
        Map<Integer, Integer> mapEva = new HashMap<>();
        for (EvaTaskDO i : taskDOList) {
            EvaTaskDO evaTaskDO = new EvaTaskDO();
            evaTaskDO.setStatus(2);
            evaTaskMapper.update(evaTaskDO, new QueryWrapper<EvaTaskDO>()
                    .eq("teacher_id", i.getTeacherId())
                    .eq("cour_inf_id", courINfo.getCourseId()));
            mapEva.put(i.getTeacherId(), i.getId());
        }

        Map<String, Map<Integer, Integer>> map = new HashMap<>();
        map.put(userDO.getName() + "老师的" + name + "课程(" + natureName + ")的上课时间被修改了", null);
        map.put("因为" + userDO.getName() + "老师的" + name + "课程(" + natureName + ")的上课时间修改，故已取消您对该课程的评教任务", mapEva);

        LogUtils.logContent(name + "上课时间信息");
        localCacheManager.invalidateCache(null, evaCacheConstants.LOG_LIST);
        localCacheManager.invalidateCache(evaCacheConstants.TASK_LIST_BY_SEM, String.valueOf(semId));
        localCacheManager.invalidateCache(null, classroomCacheConstants.ALL_CLASSROOM);
        return map;
    }
}
