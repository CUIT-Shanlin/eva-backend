package edu.cuit.infra.bccourse.support;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;
import edu.cuit.infra.dal.database.mapper.course.CourInfMapper;
import edu.cuit.zhuyimeng.framework.common.exception.UpdateException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 教室占用冲突校验器。
 *
 * <p>说明：DDD 渐进式重构阶段，仅收敛重复逻辑，不改变原有 SQL 条件与异常文案。</p>
 */
@Component
@RequiredArgsConstructor
public class ClassroomOccupancyChecker {
    private final CourInfMapper courInfMapper;

    public void assertClassroomAvailable(
            Integer week,
            Integer day,
            Integer startTime,
            Integer endTime,
            String location,
            Integer excludeCourseId,
            String errorMessage
    ) {
        QueryWrapper<CourInfDO> wrapper = new QueryWrapper<CourInfDO>()
                .eq("week", week)
                .eq("day", day)
                .eq("location", location)
                .le("start_time", endTime)
                .ge("end_time", startTime);
        if (excludeCourseId != null) {
            wrapper.and(courseWrapper -> courseWrapper.ne("course_id", excludeCourseId));
        }
        if (courInfMapper.selectOne(wrapper) != null) {
            throw new UpdateException(errorMessage);
        }
    }
}

