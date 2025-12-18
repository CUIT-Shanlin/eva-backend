package edu.cuit.bc.course.application.port;

import edu.cuit.bc.course.application.model.UpdateCourseInfoCommand;

import java.util.Map;

/**
 * 修改课程信息的持久化端口。
 *
 * <p>说明：当前处于渐进式重构阶段，返回值沿用旧系统的消息模型（用于事件化副作用），以保证行为不变。</p>
 */
public interface UpdateCourseInfoRepository {
    Map<String, Map<Integer, Integer>> update(UpdateCourseInfoCommand command);
}

