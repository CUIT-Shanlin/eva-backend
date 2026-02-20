package edu.cuit.bc.course.application.port;

import java.util.Optional;

/**
 * 课程读侧：按评教模板ID查询“任一课程ID”的查询端口（用于判断模板是否已被课程分配）。
 *
 * <p>保持行为不变（重要）：该端口用于替代“跨 BC 直连课程域 DAL（CourseMapper/CourseDO）”的用法；
 * 实现方应沿用调用方的旧实现语义（查询条件、空值表现与异常触发时机不变），且不应引入新的缓存/日志副作用。</p>
 */
public interface CourseIdByTemplateIdQueryPort {

    /**
     * 按模板ID查询任一课程ID（沿用旧实现语义，若未命中则返回 empty）。
     */
    Optional<Integer> findCourseIdByTemplateId(Integer templateId);
}
