package edu.cuit.bc.course.application.port;

import java.util.List;

/**
 * 课程详情对象（cour_inf）直查端口（供其它 BC 基础设施层使用，保持行为不变）。
 *
 * <p>约束：该端口用于替代“跨 BC 直连课程域 DAL（CourInfMapper/CourInfDO）”的用法。</p>
 *
 * <p><b>保持行为不变（重要）</b>：实现方应沿用调用方的旧实现语义（SQL、空值/异常语义与副作用顺序不变），
 * 且不应引入新的缓存命中/回源副作用。</p>
 *
 * <p>说明：返回类型使用 {@code Object}，用于避免在 application 端口中暴露 DAL DataObject 类型导致不必要的模块耦合；
 * 过渡期实际返回值为课程域 DAL 查询得到的“课程详情对象”。</p>
 */
public interface CourInfObjectDirectQueryPort {

    /**
     * 按课程详情ID查询课程详情对象（沿用旧实现语义，可能返回 null）。
     */
    Object findById(Integer courInfId);

    /**
     * 按课程ID集合查询课程详情对象列表（沿用旧实现语义）。
     */
    List<Object> findByCourseIds(List<Integer> courseIds);

    /**
     * 按课程详情ID集合查询课程详情对象列表（沿用旧实现语义）。
     */
    List<Object> findByIds(List<Integer> courInfIds);
}

