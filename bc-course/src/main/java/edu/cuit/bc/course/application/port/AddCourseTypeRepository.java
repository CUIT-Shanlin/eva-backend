package edu.cuit.bc.course.application.port;

import edu.cuit.client.dto.data.course.CourseType;

/**
 * 新增课程类型持久化端口。
 *
 * <p>说明：现阶段只做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public interface AddCourseTypeRepository {
    void add(CourseType courseType);
}

