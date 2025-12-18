package edu.cuit.bc.course.application.port;

import edu.cuit.client.dto.clientobject.course.SelfTeachCourseCO;
import edu.cuit.client.dto.clientobject.course.SelfTeachCourseTimeInfoCO;

import java.util.List;
import java.util.Map;

/**
 * 教师自助改课持久化端口。
 *
 * <p>说明：当前处于渐进式重构阶段，返回值沿用旧系统消息模型（用于后续事件化副作用），以保证行为不变。</p>
 */
public interface UpdateSelfCourseRepository {
    Map<String, Map<Integer, Integer>> update(String username, SelfTeachCourseCO selfTeachCourseCO, List<SelfTeachCourseTimeInfoCO> timeList);
}

