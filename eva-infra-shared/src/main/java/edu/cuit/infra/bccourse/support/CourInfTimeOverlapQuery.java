package edu.cuit.infra.bccourse.support;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import edu.cuit.infra.dal.database.dataobject.course.CourInfDO;

/**
 * 课程详情（cour_inf）时间段重叠查询条件构造器。
 *
 * <p>说明：DDD 渐进式重构阶段，仅收敛重复 QueryWrapper 片段，不改变原有 SQL 条件与判定边界。</p>
 */
public final class CourInfTimeOverlapQuery {
    private CourInfTimeOverlapQuery() {
    }

    /**
     * 构造“同周同天且时间段重叠”的查询条件：
     * start_time <= endTime 且 end_time >= startTime
     */
    public static QueryWrapper<CourInfDO> overlap(Integer week, Integer day, Integer startTime, Integer endTime) {
        return new QueryWrapper<CourInfDO>()
                .eq("week", week)
                .eq("day", day)
                .le("start_time", endTime)
                .ge("end_time", startTime);
    }
}
