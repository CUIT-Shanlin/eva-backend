package edu.cuit.bc.course.application.port;

import java.util.List;
import java.util.Optional;

/**
 * 课程详情（cour_inf）时间片查询端口。
 * <p>
 * 用途：为“跨 BC 直连清零”提供最小查询能力，避免其它 BC 直接依赖课程域 DAL Mapper/DO。
 * </p>
 */
public interface CourInfTimeSlotQueryPort {

    /**
     * 按课程详情ID查询时间片（用于替换 CourInfMapper.selectById）。
     */
    Optional<CourInfTimeSlot> findByCourInfId(Integer courInfId);

    /**
     * 按课程ID集合查询时间片集合（用于替换 CourInfMapper.selectList(in course_id)）。
     */
    List<CourInfTimeSlot> findByCourseIds(List<Integer> courseIds);

    /**
     * 按课程详情ID集合查询时间片集合（用于替换 CourInfMapper.selectList(in id)）。
     */
    List<CourInfTimeSlot> findByCourInfIds(List<Integer> courInfIds);

    record CourInfTimeSlot(
            Integer id,
            Integer courseId,
            Integer week,
            Integer day,
            Integer startTime,
            Integer endTime
    ) {
    }
}

