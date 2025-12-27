package edu.cuit.bc.course.application.port;

import java.util.Map;

/**
 * 教师自助删课持久化端口。
 *
 * <p>说明：当前处于渐进式重构阶段，返回值沿用旧系统消息模型（用于后续事件化副作用），以保证行为不变。</p>
 */
public interface DeleteSelfCourseRepository {
    Map<String, Map<Integer, Integer>> delete(String username, Integer courseId);
}

