package edu.cuit.bc.course.application.port;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import edu.cuit.infra.dal.database.dataobject.course.CourseDO;
import java.util.List;

/**
 * 课程读侧：按课程查询条件直查课程ID列表的端口（用于跨 BC 去课程域 DAL 直连）。
 *
 * <p><b>保持行为不变（重要）</b>：该端口用于收敛“仅为拿课程ID列表而先查 CourseDO 再映射”的跨 BC 读侧调用点。
 * 端口适配器应沿用调用方旧实现语义（查询条件、结果顺序与空值表现不变），且不应引入新的缓存/日志副作用。</p>
 *
 * <p>说明：当调用方需要复用既有的 MyBatis-Plus 条件（Wrapper）时，可优先通过该端口直接获取课程ID列表，
 * 以避免在调用侧重复进行 {@code CourseDO::getId} 映射。</p>
 */
public interface CourseIdsByCourseWrapperDirectQueryPort {

    /**
     * 按条件查询课程ID列表（沿用旧实现语义；若无命中则返回空列表）。
     */
    List<Integer> findCourseIds(Wrapper<CourseDO> queryWrapper);
}

