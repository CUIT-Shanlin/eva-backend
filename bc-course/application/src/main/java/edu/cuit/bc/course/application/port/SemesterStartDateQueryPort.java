package edu.cuit.bc.course.application.port;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 课程读侧：按学期ID查询学期开始日期的查询端口（渐进式重构用）。
 *
 * <p>约束：仅做结构性抽象，不改变既有查询语义、结果顺序与空值表现；具体行为由端口适配器原样搬运历史实现保证。</p>
 */
public interface SemesterStartDateQueryPort {
    Optional<LocalDate> findStartDateBySemesterId(Integer semesterId);
}

