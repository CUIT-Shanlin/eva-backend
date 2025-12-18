package edu.cuit.bc.course.application.port;

import java.util.Map;

/**
 * 连带删除一门课程持久化端口。
 *
 * <p>说明：当前处于渐进式重构阶段，返回值沿用旧系统消息模型（用于后续事件化副作用），以保证行为不变。</p>
 */
public interface DeleteCourseRepository {
    Map<String, Map<Integer, Integer>> delete(Integer semesterId, Integer courseId);
}

