package edu.cuit.bc.course.application.port;

import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeCO;
import edu.cuit.client.dto.cmd.course.UpdateCourseCmd;

import java.util.List;

/**
 * 批量新建多节课（新课程）持久化端口。
 *
 * <p>说明：现阶段只做 DDD 渐进式重构（收敛入口/抽离端口），不做业务逻辑优化，行为以旧系统为准。</p>
 */
public interface AddNotExistCoursesDetailsRepository {
    void add(Integer semesterId, Integer teacherId, UpdateCourseCmd courseInfo, List<SelfTeachCourseTimeCO> dateArr);
}

