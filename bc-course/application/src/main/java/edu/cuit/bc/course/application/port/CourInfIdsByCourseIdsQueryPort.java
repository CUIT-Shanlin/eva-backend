package edu.cuit.bc.course.application.port;

import java.util.List;

/**
 * 通过课程ID集合（course.id）查询课程详情ID集合（cour_inf.id）的端口。
 * <p>
 * 用途：为“跨 BC 直连清零”提供最小查询能力，避免其它 BC 直接依赖课程域 DAL Mapper/DO。
 * </p>
 */
public interface CourInfIdsByCourseIdsQueryPort {
    List<Integer> findCourInfIdsByCourseIds(List<Integer> courseIds);
}

