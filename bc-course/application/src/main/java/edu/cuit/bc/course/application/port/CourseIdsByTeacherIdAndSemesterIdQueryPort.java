package edu.cuit.bc.course.application.port;

import java.util.List;

/**
 * 课程读侧：按 teacherId + semesterId 查询课程ID集合的查询端口（用于跨 BC 去课程域 DAL 直连）。
 *
 * <p><b>保持行为不变（重要）</b>：该端口用于替代“跨 BC 直连课程域 DAL（CourseMapper/CourseDO）”的用法。
 * 端口适配器应沿用调用方旧实现语义（查询条件、结果顺序与空值表现不变），且不应引入新的缓存/日志副作用。</p>
 */
public interface CourseIdsByTeacherIdAndSemesterIdQueryPort {

    /**
     * 按 teacherId + semesterId 查询课程ID列表（沿用旧实现语义；若无命中则返回空列表）。
     */
    List<Integer> findCourseIdsByTeacherIdAndSemesterId(Integer teacherId, Integer semesterId);
}
