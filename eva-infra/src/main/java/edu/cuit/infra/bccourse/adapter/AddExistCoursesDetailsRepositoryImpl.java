package edu.cuit.infra.bccourse.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.bc.course.application.port.AddExistCoursesDetailsRepository;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeCO;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.course.SubjectDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.infra.dal.database.mapper.course.CourseMapper;
import edu.cuit.infra.dal.database.mapper.course.SubjectMapper;
import edu.cuit.infra.enums.cache.ClassroomCacheConstants;
import edu.cuit.zhuyimeng.framework.cache.LocalCacheManager;
import edu.cuit.zhuyimeng.framework.common.exception.QueryException;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import edu.cuit.zhuyimeng.framework.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * bc-course：批量新建多节课（已有课程）端口适配器（复用现有表结构与规则，行为保持不变）。
 */
@Component
@RequiredArgsConstructor
public class AddExistCoursesDetailsRepositoryImpl implements AddExistCoursesDetailsRepository {
    private final CourInfMapper courInfMapper;
    private final CourseMapper courseMapper;
    private final SubjectMapper subjectMapper;
    private final LocalCacheManager localCacheManager;
    private final ClassroomCacheConstants classroomCacheConstants;

    @Override
    @Transactional
    public void add(Integer courseId, SelfTeachCourseTimeCO timeCO) {
        for (Integer week : timeCO.getWeeks()) {
            judgeAlsoHasLocation(week, timeCO);
            CourInfDO courInfDO = new CourInfDO();
            courInfDO.setCourseId(courseId);
            courInfDO.setWeek(week);
            courInfDO.setDay(timeCO.getDay());
            courInfDO.setStartTime(timeCO.getStartTime());
            courInfDO.setEndTime(timeCO.getEndTime());
            courInfDO.setLocation(timeCO.getClassroom());
            courInfDO.setCreateTime(LocalDateTime.now());
            courInfDO.setUpdateTime(LocalDateTime.now());
            courInfMapper.insert(courInfDO);
        }
        CourseDO courseDO = courseMapper.selectById(courseId);
        if (courseDO == null) throw new QueryException("不存在对应的课程");
        SubjectDO subjectDO = subjectMapper.selectById(courseDO.getSubjectId());
        if (subjectDO == null) throw new QueryException("不存在对应的课程的科目");
        LogUtils.logContent(subjectDO.getName() + "(ID:" + courseDO.getId() + ")的课程的课数");
        localCacheManager.invalidateCache(null, classroomCacheConstants.ALL_CLASSROOM);
    }

    private void judgeAlsoHasLocation(Integer week, SelfTeachCourseTimeCO timeCO) {
        CourInfDO courInfDO = courInfMapper.selectOne(new QueryWrapper<CourInfDO>()
                .eq("week", week)
                .eq("day", timeCO.getDay())
                .eq("location", timeCO.getClassroom())
                .le("start_time", timeCO.getEndTime())
                .ge("end_time", timeCO.getStartTime()));
        if (courInfDO != null) {
            throw new UpdateException("该时间段教室冲突，请修改时间");
        }
    }
}

