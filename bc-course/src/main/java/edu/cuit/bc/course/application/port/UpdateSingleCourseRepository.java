package edu.cuit.bc.course.application.port;

import edu.cuit.bc.course.application.model.UpdateSingleCourseCommand;

import java.util.Map;

/**
 * 改课（修改单节课课次信息）持久化端口。
 *
 * <p>说明：渐进式重构阶段，为保持行为不变，返回值沿用旧系统消息模型。</p>
 */
public interface UpdateSingleCourseRepository {
    Map<String, Map<Integer, Integer>> update(UpdateSingleCourseCommand command);
}

