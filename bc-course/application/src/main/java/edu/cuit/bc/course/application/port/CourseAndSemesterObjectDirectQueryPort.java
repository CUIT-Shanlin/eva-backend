package edu.cuit.bc.course.application.port;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import edu.cuit.infra.dal.database.dataobject.course.SemesterDO;
import java.util.List;

/**
 * 课程读侧：课程/学期基础数据对象的直查端口（用于跨 BC 去课程域 DAL 直连）。
 *
 * <p><b>保持行为不变（重要）</b>：该端口用于替代“跨 BC 直连课程域 DAL（CourseMapper/SemesterMapper）”的用法。
 * 端口适配器应沿用调用方旧实现语义（查询条件、结果顺序、空值表现与异常触发时机不变），且不应引入新的缓存/日志副作用。</p>
 */
public interface CourseAndSemesterObjectDirectQueryPort {

    /**
     * 按条件查询课程列表（沿用旧实现语义；若无命中则返回空列表）。
     */
    List<CourseDO> findCourseList(Wrapper<CourseDO> queryWrapper);

    /**
     * 按课程ID查询课程对象（沿用旧实现语义；若无命中则返回 null）。
     */
    CourseDO findCourseById(Integer courseId);

    /**
     * 按条件查询单条课程对象（沿用旧实现语义；若无命中则返回 null）。
     */
    CourseDO findOneCourse(Wrapper<CourseDO> queryWrapper);

    /**
     * 按学期ID查询学期对象（沿用旧实现语义；若无命中则返回 null）。
     */
    SemesterDO findSemesterById(Integer semesterId);
}

