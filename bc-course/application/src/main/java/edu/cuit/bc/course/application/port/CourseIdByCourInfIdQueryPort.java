package edu.cuit.bc.course.application.port;

import java.util.Optional;

/**
 * 通过课程详情ID（cour_inf.id）查询课程ID的端口。
 * <p>
 * 用途：为“跨 BC 直连清零”提供最小查询能力，避免其它 BC 直接依赖课程域 DAL Mapper/DO。
 * </p>
 */
public interface CourseIdByCourInfIdQueryPort {
    Optional<Integer> findCourseIdByCourInfId(Integer courInfId);
}

